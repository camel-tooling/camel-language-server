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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cameltooling.lsp.internal.completion.traits.CamelKTraitManager;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;

public class VSCodeTasksCompletionProcessor {

	private TextDocumentItem textDocumentItem;

	public VSCodeTasksCompletionProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		List<CompletionItem> completions = new ArrayList<>();
		// Very lazy check because I have not found an easy way to have a JSon Parser with line number
		if (textDocumentItem.getText().contains("traits")) {
			String line = new ParserFileHelperUtil().getLine(textDocumentItem, position);
			Map<String, JSONSchemaProps> traits = CamelKTraitManager.getTraits();
			if('.' == line.charAt(position.getCharacter() - 1)) {
				int startOfTrait = line.substring(0, position.getCharacter() - 1).lastIndexOf("\"");
				if (startOfTrait != -1) {
					String traitName = line.substring(startOfTrait + 1, position.getCharacter() - 1);
					JSONSchemaProps trait = traits.get(traitName);
					Map<String,JSONSchemaProps> properties = trait.getProperties();
					for (Entry<String, JSONSchemaProps> property : properties.entrySet()) {
						CompletionItem completion = createCompletionItemForProperty(position, property);
						completions.add(completion);
					}
				}
			} else {
				for (Entry<String, JSONSchemaProps> traitEntry : traits.entrySet()) {
					CompletionItem completion = createCompletionitemForTrait(position, traitEntry);
					completions.add(completion);
				}
			}
		}
		return CompletableFuture.completedFuture(completions);
	}

	private CompletionItem createCompletionitemForTrait(Position position, Entry<String, JSONSchemaProps> traitEntry) {
		CompletionItem completion = new CompletionItem(traitEntry.getKey());
		completion.setKind(CompletionItemKind.Snippet);
		Set<String> properties = traitEntry.getValue().getProperties().keySet();
		String propertiesChoice;
		if (!properties.isEmpty()) {
			propertiesChoice = "${1|" + String.join(",", properties) + "|}";
		} else {
			propertiesChoice = "";
		}
		String insertText = "\"" + traitEntry.getKey() + "." + propertiesChoice + "=\"";
		completion.setInsertTextFormat(InsertTextFormat.Snippet);
		applyTextEdit(position, completion, insertText);
		completion.setDocumentation(traitEntry.getValue().getDescription());
		return completion;
	}

	private CompletionItem createCompletionItemForProperty(Position position, Entry<String, JSONSchemaProps> property) {
		String label = property.getKey();
		CompletionItem completion = new CompletionItem(label);
		JsonNode defaultPropertyValue = property.getValue().getDefault();
		String insertText;
		if (defaultPropertyValue != null) {
			insertText = label + "=" + defaultPropertyValue;
		} else {
			insertText = label;
		}
		applyTextEdit(position, completion, insertText);
		completion.setSortText("${1" + label);
		completion.setDocumentation(property.getValue().getDescription());
		return completion;
	}

	private void applyTextEdit(Position position, CompletionItem completion, String insertText) {
		Position positionLSP = new Position(position.getLine(), position.getCharacter());
		completion.setTextEdit(Either.forLeft(new TextEdit(new Range(positionLSP, positionLSP), insertText)));
	}

}
