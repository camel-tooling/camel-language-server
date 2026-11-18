/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.util.StringHelper;
import org.apache.camel.v1.kameletspec.Definition;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.catalog.util.YamlDslKeys;

/**
 * Completion and hover for a Kamelet name written as the value of a key of the Camel YAML DSL: the {@code templateId}
 * in the {@code parameters} of a {@code kamelet} URI without path, which is how Kaoto references a Kamelet, and the
 * {@code name} of the Kamelet EIP. The name in a {@code kamelet:} URI is handled with the other endpoint URIs.
 */
public class CamelYamlKameletNameProcessor {

	private static final String TEMPLATE_ID = "templateId";

	private final YamlDslLines lines;
	private final KameletsCatalogManager kameletsCatalogManager;

	public CamelYamlKameletNameProcessor(YamlDslLines lines, KameletsCatalogManager kameletsCatalogManager) {
		this.lines = lines;
		this.kameletsCatalogManager = kameletsCatalogManager;
	}

	/**
	 * @return whether the cursor is after the key of a Kamelet name
	 */
	public boolean isOnKameletName(Position position) {
		return findKameletName(position) != null;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		KameletName kameletName = findKameletName(position);
		if (kameletName == null) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		List<CompletionItem> items = new KameletTemplateIdCompletionProvider(kameletsCatalogManager).get(kameletName.producer, kameletName.range);
		if (kameletName.missingSpace) {
			// a value right after the separator would be read as a part of the key
			items.stream().map(item -> item.getTextEdit().getLeft()).forEach(edit -> edit.setNewText(" " + edit.getNewText()));
		}
		return CompletableFuture.completedFuture(items);
	}

	public CompletableFuture<Hover> getHover(Position position) {
		KameletName kameletName = findKameletName(position);
		if (kameletName == null
				|| position.getCharacter() < kameletName.range.getStart().getCharacter()
				|| position.getCharacter() >= kameletName.range.getEnd().getCharacter()) {
			return CompletableFuture.completedFuture(null);
		}
		Definition definition = kameletsCatalogManager.getKameletDefinition(kameletName.value);
		if (definition == null || definition.getDescription() == null) {
			return CompletableFuture.completedFuture(null);
		}
		return CompletableFuture.completedFuture(new Hover(new MarkupContent(MarkupKind.PLAINTEXT, definition.getDescription()), kameletName.range));
	}

	/**
	 * @return the Kamelet name written on the line of the cursor, when the cursor is after its key, or null
	 */
	private KameletName findKameletName(Position position) {
		int lineNumber = position.getLine();
		String key = lines.key(lineNumber);
		if (key == null || YamlDslLines.separatorIndex(lines.contentBeforeCursor(position)) <= 0 || lines.isInsideBlockScalar(position)) {
			return null;
		}
		int parent = lines.parent(lineNumber);
		if (parent < 0 || !lines.isBlockKey(parent)) {
			return null;
		}
		String parentKey = lines.key(parent);
		Boolean producer = null;
		if (TEMPLATE_ID.equals(StringHelper.dashToCamelCase(key)) && YamlDslKeys.PARAMETERS.equals(parentKey)) {
			producer = isProducerWithKameletUriWithoutPath(lines.parent(parent));
		} else if (YamlDslKeys.NAME.equals(key) && YamlDslKeys.KAMELET.equals(parentKey) && lines.isListItem(parent)) {
			// the Kamelet EIP calls a Kamelet like a to
			producer = Boolean.TRUE;
		}
		return producer != null ? createKameletName(lineNumber, producer) : null;
	}

	/**
	 * @param owner the line of the endpoint owning the parameters
	 * @return whether the endpoint is a producer when its URI is kamelet without path, else null
	 */
	private Boolean isProducerWithKameletUriWithoutPath(int owner) {
		if (owner < 0) {
			return null;
		}
		List<Integer> mappingLines = new ArrayList<>(lines.children(owner));
		if (lines.isListItem(owner)) {
			// the first key of a sequence entry is written on the line of the entry
			mappingLines.add(owner);
		}
		for (int line : mappingLines) {
			if (YamlDslKeys.URI.equals(lines.key(line))) {
				return YamlDslKeys.KAMELET.equals(YamlDslLines.unquote(lines.value(line)))
						? !YamlDslKeys.CONSUMER_ENDPOINTS.contains(lines.key(owner))
						: null;
			}
		}
		return null;
	}

	/**
	 * @return the Kamelet name of the line: its value, inside its quotes and without trailing comment
	 */
	private KameletName createKameletName(int lineNumber, boolean producer) {
		String line = lines.line(lineNumber);
		int valueColumn = lines.contentColumn(lineNumber) + YamlDslLines.separatorIndex(lines.content(lineNumber)) + 1;
		int start = valueColumn;
		while (start < line.length() && Character.isWhitespace(line.charAt(start))) {
			start++;
		}
		int end;
		if (start < line.length() && (line.charAt(start) == '"' || line.charAt(start) == '\'')) {
			char quote = line.charAt(start);
			start++;
			int closingQuote = line.indexOf(quote, start);
			end = closingQuote >= 0 ? closingQuote : line.length();
		} else if (start < line.length() && line.charAt(start) == '#') {
			// only a comment
			end = start;
		} else {
			int comment = line.indexOf(" #", start);
			end = comment >= 0 ? comment : line.length();
			while (end > start && Character.isWhitespace(line.charAt(end - 1))) {
				end--;
			}
		}
		Range range = new Range(new Position(lineNumber, start), new Position(lineNumber, end));
		return new KameletName(line.substring(start, end), producer, range, valueColumn >= line.length());
	}

	private static class KameletName {
		private final String value;
		private final boolean producer;
		private final Range range;
		private final boolean missingSpace;

		KameletName(String value, boolean producer, Range range, boolean missingSpace) {
			this.value = value;
			this.producer = producer;
			this.range = range;
			this.missingSpace = missingSpace;
		}
	}
}
