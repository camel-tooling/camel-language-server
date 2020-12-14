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
package com.github.cameltooling.lsp.internal.instancemodel.propertiesfile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.MainModel;
import org.apache.camel.util.StringHelper;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;

import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;
import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;
import com.google.gson.Gson;

/**
 * Represents one key in properties file.
 * For instance, with "camel.component.timer.delay=1000",
 * it is used to represents "camel.component.timer.delay"
 * 
 */
public class CamelPropertyKeyInstance implements ILineRangeDefineable {
	
	static final String CAMEL_KEY_PREFIX = "camel.";
	static final String CAMEL_COMPONENT_KEY_PREFIX = "camel.component.";
	public static final String CAMEL_SINK_KEY_PREFIX = "camel.sink.";
	public static final String CAMEL_SOURCE_KEY_PREFIX = "camel.source.";
	
	private String camelPropertyKey;
	private CamelGroupPropertyKey propertyGroup;
	private CamelComponentPropertyKey camelComponentPropertyKey;
	private CamelPropertyEntryInstance camelPropertyEntryInstance;
	private CamelSinkOrSourcePropertyKey camelSinkOrSourcePropertyKey;

	public CamelPropertyKeyInstance(String camelPropertyFileKey, CamelPropertyEntryInstance camelPropertyEntryInstance, TextDocumentItem textDocumentItem) {
		this.camelPropertyKey = camelPropertyFileKey;
		this.camelPropertyEntryInstance = camelPropertyEntryInstance;
		if (camelPropertyFileKey.startsWith(CAMEL_COMPONENT_KEY_PREFIX)) {
			camelComponentPropertyKey = new CamelComponentPropertyKey(camelPropertyFileKey.substring(CAMEL_COMPONENT_KEY_PREFIX.length()), this);
		} else if(camelPropertyFileKey.startsWith(CAMEL_SINK_KEY_PREFIX)) {
			camelSinkOrSourcePropertyKey = new CamelSinkOrSourcePropertyKey(camelPropertyFileKey.substring(CAMEL_SINK_KEY_PREFIX.length()), this, textDocumentItem, CAMEL_SINK_KEY_PREFIX);
		} else if(camelPropertyFileKey.startsWith(CAMEL_SOURCE_KEY_PREFIX)) {
			camelSinkOrSourcePropertyKey = new CamelSinkOrSourcePropertyKey(camelPropertyFileKey.substring(CAMEL_SOURCE_KEY_PREFIX.length()), this, textDocumentItem, CAMEL_SOURCE_KEY_PREFIX);
		}
		if(camelPropertyKey.startsWith(CAMEL_KEY_PREFIX)) {
			propertyGroup = new CamelGroupPropertyKey(camelPropertyFileKey.substring(CAMEL_KEY_PREFIX.length()), this);
		}
	}

	public int getEndposition() {
		return camelPropertyEntryInstance.getStartPositionInLine() + camelPropertyKey.length();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog, CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		int indexOfFirstDot = camelPropertyKey.indexOf('.');
		int indexOfSecondDot = indexOfFirstDot != -1 ? camelPropertyKey.indexOf('.', indexOfFirstDot + 1) : -1;
		if(isBeforeFirstDot(position, indexOfFirstDot)) {
			CompletionItem completionItem = new CompletionItem("camel");
			String insertText = indexOfFirstDot != -1 ? "camel" : CAMEL_KEY_PREFIX;
			completionItem.setInsertText(insertText);
			completionItem.setTextEdit(new TextEdit(new Range(new Position(getLine(), getStartPositionInLine()), new Position(getLine(), indexOfFirstDot != -1 ? getStartPositionInLine() + indexOfFirstDot : getEndPositionInLine())), insertText));
			return CompletableFuture.completedFuture(Collections.singletonList(completionItem));
		} else if (isBetweenFirstAndSecondDotOfCamelPropertyKey(position, indexOfSecondDot)) {
			return getTopLevelCamelCompletion(camelCatalog, indexOfSecondDot, position.getCharacter());
		} else if(camelComponentPropertyKey != null && camelComponentPropertyKey.isInRange(position.getCharacter())) {
			return camelComponentPropertyKey.getCompletions(position, camelCatalog);
		} else if(camelSinkOrSourcePropertyKey != null && camelSinkOrSourcePropertyKey.isInRange(position.getCharacter())) {
			return camelSinkOrSourcePropertyKey.getCompletions(position, camelKafkaConnectorManager);
		} else if(propertyGroup != null && propertyGroup.isInRange(position.getCharacter())) {
			return propertyGroup.getCompletions(position, camelCatalog);
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	private boolean isBetweenFirstAndSecondDotOfCamelPropertyKey(Position position, int indexOfSecondDot) {
		return getStartPositionInLine() + CAMEL_KEY_PREFIX.length() <= position.getCharacter()
				&& camelPropertyKey.startsWith(CAMEL_KEY_PREFIX)
				&& position.getCharacter() <= getStartPositionInLine() + Math.max(CAMEL_KEY_PREFIX.length(), indexOfSecondDot);
	}

	private boolean isBeforeFirstDot(Position position, int indexOfFirstDot) {
		return getStartPositionInLine() <= position.getCharacter() && position.getCharacter() <= getStartPositionInLine() + Math.max(0, indexOfFirstDot);
	}

	protected CompletableFuture<List<CompletionItem>> getTopLevelCamelCompletion(CompletableFuture<CamelCatalog> camelCatalog, int indexOfSecondDot, int completionPositionRequest) {
		String filterString = camelPropertyKey.substring(CAMEL_KEY_PREFIX.length(), completionPositionRequest - getStartPositionInLine());
		return camelCatalog.thenApply(catalog -> {
			MainModel mainModel = new Gson().fromJson(catalog.mainJsonSchema(), MainModel.class);
			List<CompletionItem> allCompletionItems = new ArrayList<>();
			allCompletionItems.addAll(createGroupCompletionFromMainModel(mainModel, indexOfSecondDot));
			allCompletionItems.add(createCompletionItemForCamelComponent(indexOfSecondDot));
			return allCompletionItems.stream().filter(FilterPredicateUtils.matchesCompletionFilter(filterString)).collect(Collectors.toList());
		});
	}

	private CompletionItem createCompletionItemForCamelComponent(int indexOfSecondDot) {
		CompletionItem completionItem = new CompletionItem("component");
		String insertText = indexOfSecondDot != -1 ? "component" : "component.";
		completionItem.setInsertText(insertText);
		Position start = new Position(getLine(), getStartPositionInLine() + CAMEL_KEY_PREFIX.length());
		Position end = new Position(getLine(), indexOfSecondDot != -1 ? getStartPositionInLine() + indexOfSecondDot : getEndPositionInLine());
		Range range = new Range(start, end);
		TextEdit textEdit = new TextEdit(range, insertText);
		completionItem.setTextEdit(textEdit);
		return completionItem;
	}

	private List<CompletionItem> createGroupCompletionFromMainModel(MainModel mainModel, int indexOfSecondDot) {
		return mainModel.getGroups().stream().map(group -> {
			String realGroupName = group.getName().replaceFirst(CAMEL_KEY_PREFIX, "");
			CompletionItem completionItem = new CompletionItem(realGroupName);
			completionItem.setDocumentation(group.getDescription());
			String insertText = indexOfSecondDot != -1 ? realGroupName : realGroupName + ".";
			completionItem.setInsertText(insertText);
			Position start = new Position(getLine(), getStartPositionInLine() + CAMEL_KEY_PREFIX.length());
			Position end = new Position(getLine(), indexOfSecondDot != -1 ? getStartPositionInLine() + indexOfSecondDot : getEndPositionInLine());
			Range range = new Range(start, end);
			completionItem.setTextEdit(new TextEdit(range, insertText));
			return completionItem;
		}).collect(Collectors.toList());
	}

	public String getCamelPropertyKey() {
		return camelPropertyKey;
	}

	public CamelComponentPropertyKey getCamelComponentPropertyKey() {
		return camelComponentPropertyKey;
	}

	public CamelPropertyEntryInstance getCamelPropertyEntryInstance() {
		return camelPropertyEntryInstance;
	}

	public int getLine() {
		return camelPropertyEntryInstance.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return camelPropertyEntryInstance.getStartPositionInLine();
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + camelPropertyKey.length();
	}

	public CompletableFuture<Hover> getHover(Position position, CompletableFuture<CamelCatalog> camelCatalog, CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		if(camelComponentPropertyKey != null && camelComponentPropertyKey.isInRange(position.getCharacter())) {
			return camelComponentPropertyKey.getHover(position, camelCatalog);
		} else if(camelSinkOrSourcePropertyKey != null && camelSinkOrSourcePropertyKey.isInRange(position.getCharacter())) {
			return camelSinkOrSourcePropertyKey.getHover(camelKafkaConnectorManager);
		} else if(propertyGroup != null && propertyGroup.isInRange(position.getCharacter())) {
			return propertyGroup.getHover(position, camelCatalog);
		}
		return CompletableFuture.completedFuture(null);
	}

	public boolean shouldUseDashedCase() {
		return camelPropertyEntryInstance.shouldUseDashedCase();
	}

	public Collection<Diagnostic> validate(CamelKafkaConnectorCatalogManager camelKafkaConnectorManager, Set<CamelPropertyEntryInstance> allCamelPropertyEntriesOfTheFile) {
		Collection<Diagnostic> diagnostics = new HashSet<>();
		if(camelSinkOrSourcePropertyKey != null) {
			diagnostics.addAll(camelSinkOrSourcePropertyKey.validate(camelKafkaConnectorManager, allCamelPropertyEntriesOfTheFile));
		}
		if(camelPropertyKey != null && camelPropertyKey.startsWith(CAMEL_KEY_PREFIX)) {
			Optional<String> duplicateByDashCamelNotationDifference = allCamelPropertyEntriesOfTheFile.stream()
				.map(CamelPropertyEntryInstance::getCamelPropertyKeyInstance)
				.filter(Objects::nonNull)
				.map(CamelPropertyKeyInstance::getCamelPropertyKey)
				.filter(Objects::nonNull)
				.filter(iterCamelPropertyKey ->
					!iterCamelPropertyKey.equals(camelPropertyKey)
					&& StringHelper.dashToCamelCase(iterCamelPropertyKey).equals(StringHelper.dashToCamelCase(camelPropertyKey)))
				.findAny();
			if(duplicateByDashCamelNotationDifference.isPresent()) {
				diagnostics.add(new Diagnostic(
						new Range(new Position(getLine(), getStartPositionInLine()), new Position(getLine(), getEndPositionInLine())),
						"Duplicated properties using different Dash/camel case notation: " + duplicateByDashCamelNotationDifference.get(),
						DiagnosticSeverity.Error,
						DiagnosticService.APACHE_CAMEL_VALIDATION));
			}
		}
		return diagnostics;
	}

}
