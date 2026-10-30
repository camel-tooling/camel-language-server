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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.MainModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.RuntimeConfigurationProperties;
import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.RuntimeConfigurationProperty;
import com.github.cameltooling.lsp.internal.catalog.util.ModelDocumentationHelper;
import com.github.cameltooling.lsp.internal.catalog.util.StringUtils;
import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

/**
 * Represents one key in properties file.
 * For instance, with "camel.component.timer.delay=1000",
 * it is used to represents "camel.component.timer.delay"
 * 
 */
public class CamelPropertyKeyInstance implements ILineRangeDefineable {
	
	static final String CAMEL_KEY_PREFIX = "camel.";
	static final String CAMEL_COMPONENT_KEY_PREFIX = "camel.component.";
	
	private String camelPropertyKey;
	private CamelGroupPropertyKey propertyGroup;
	private CamelComponentPropertyKey camelComponentPropertyKey;
	private CamelPropertyEntryInstance camelPropertyEntryInstance;

	public CamelPropertyKeyInstance(String camelPropertyFileKey, CamelPropertyEntryInstance camelPropertyEntryInstance) {
		this.camelPropertyKey = camelPropertyFileKey;
		this.camelPropertyEntryInstance = camelPropertyEntryInstance;
		if (camelPropertyFileKey.startsWith(CAMEL_COMPONENT_KEY_PREFIX)) {
			camelComponentPropertyKey = new CamelComponentPropertyKey(camelPropertyFileKey.substring(CAMEL_COMPONENT_KEY_PREFIX.length()), this);
		}
		if(camelPropertyKey.startsWith(CAMEL_KEY_PREFIX)) {
			propertyGroup = new CamelGroupPropertyKey(camelPropertyFileKey.substring(CAMEL_KEY_PREFIX.length()), this);
		}
	}

	public int getEndposition() {
		return camelPropertyEntryInstance.getStartPositionInLine() + camelPropertyKey.length();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog, boolean markdown) {
		return getCatalogCompletions(position, camelCatalog).thenCombine(camelCatalog, (completions, catalog) -> {
			Set<String> catalogLabels = completions.stream().map(CompletionItem::getLabel).collect(Collectors.toSet());
			List<CompletionItem> runtimeCompletions = getRuntimeCompletions(position, RuntimeConfigurationProperties.of(catalog), markdown).stream()
					.filter(completionItem -> !catalogLabels.contains(completionItem.getLabel()))
					.collect(Collectors.toList());
			if (runtimeCompletions.isEmpty()) {
				return completions;
			}
			List<CompletionItem> allCompletions = new ArrayList<>(completions);
			allCompletions.addAll(runtimeCompletions);
			return allCompletions;
		});
	}

	private CompletableFuture<List<CompletionItem>> getCatalogCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		int indexOfFirstDot = camelPropertyKey.indexOf('.');
		int indexOfSecondDot = indexOfFirstDot != -1 ? camelPropertyKey.indexOf('.', indexOfFirstDot + 1) : -1;
		if(isBeforeFirstDot(position, indexOfFirstDot)) {
			CompletionItem completionItem = new CompletionItem("camel");
			String insertText = indexOfFirstDot != -1 ? "camel" : CAMEL_KEY_PREFIX;
			completionItem.setInsertText(insertText);
			completionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(getLine(), getStartPositionInLine()), new Position(getLine(), indexOfFirstDot != -1 ? getStartPositionInLine() + indexOfFirstDot : getEndPositionInLine())), insertText)));
			return CompletableFuture.completedFuture(Collections.singletonList(completionItem));
		} else if (isBetweenFirstAndSecondDotOfCamelPropertyKey(position, indexOfSecondDot)) {
			return getTopLevelCamelCompletion(camelCatalog, indexOfSecondDot, position.getCharacter());
		} else if(camelComponentPropertyKey != null && camelComponentPropertyKey.isInRange(position.getCharacter())) {
			return camelComponentPropertyKey.getCompletions(position, camelCatalog);
		} else if(propertyGroup != null && propertyGroup.isInRange(position.getCharacter())) {
			return propertyGroup.getCompletions(position, camelCatalog);
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	/**
	 * @return the segment following the part of the key before the position, in the keys of the configuration properties
	 * that Camel JBang and the runtime add: a property, or a group of properties when more segments follow, as the
	 * Camel Main options are proposed after their group
	 */
	private List<CompletionItem> getRuntimeCompletions(Position position, RuntimeConfigurationProperties runtimeProperties, boolean markdown) {
		String key = camelPropertyKey.stripLeading();
		int keyStart = getEndPositionInLine() - key.length();
		int positionInKey = position.getCharacter() - keyStart;
		if (runtimeProperties.isEmpty() || positionInKey < 0 || positionInKey > key.length() || key.startsWith("#") || key.startsWith("!")) {
			return Collections.emptyList();
		}
		String writtenKey = key.substring(0, positionInKey);
		long writtenDots = writtenKey.chars().filter(c -> c == '.').count();
		Position segmentStart = new Position(getLine(), keyStart + writtenKey.lastIndexOf('.') + 1);
		Range propertyRange = new Range(segmentStart, new Position(getLine(), Math.max(position.getCharacter(), keyStart + key.stripTrailing().length())));
		int nextDot = key.indexOf('.', positionInKey);
		// a group replaces the segment at the position only, the segments after it stay
		Range groupRange = nextDot != -1 ? new Range(segmentStart, new Position(getLine(), keyStart + nextDot)) : propertyRange;
		boolean hasValue = camelPropertyEntryInstance.hasValue();
		Map<String, CompletionItem> completions = new LinkedHashMap<>();
		Map<String, Set<String>> groupRuntimes = new HashMap<>();
		for (RuntimeConfigurationProperty property : runtimeProperties.findStartingWith(writtenKey)) {
			String name = property.getName();
			int segmentIndex = indexAfterDots(name, writtenDots);
			int segmentEnd = name.indexOf('.', segmentIndex);
			if (segmentEnd == -1) {
				String segment = name.substring(segmentIndex);
				completions.putIfAbsent(segment, createPropertyCompletion(property, segment, new TextEdit(propertyRange, hasValue ? segment : segment + "="), markdown));
			} else {
				String group = name.substring(segmentIndex, segmentEnd);
				completions.computeIfAbsent(group + ".", groupKey -> createGroupCompletion(group, runtimeProperties.getGroupDescription(name.substring(0, segmentEnd)),
						new TextEdit(groupRange, nextDot != -1 ? group : group + ".")));
				groupRuntimes.computeIfAbsent(group + ".", groupKey -> new HashSet<>()).add(property.getRuntime());
			}
		}
		// the group of the properties of a single runtime tells which one
		groupRuntimes.forEach((groupKey, runtimes) -> {
			if (runtimes.size() == 1) {
				completions.get(groupKey).setDetail(runtimes.iterator().next());
			}
		});
		return new ArrayList<>(completions.values());
	}

	private CompletionItem createPropertyCompletion(RuntimeConfigurationProperty property, String segment, TextEdit textEdit, boolean markdown) {
		CompletionItem completionItem = new CompletionItem(segment);
		completionItem.setKind(CompletionItemKind.Property);
		completionItem.setDetail(property.getRuntime());
		completionItem.setDocumentation(ModelDocumentationHelper.getConfigurationPropertyDocumentation(property, markdown));
		CompletionResolverUtils.applyDeprecation(completionItem, property.isDeprecated());
		completionItem.setTextEdit(Either.forLeft(textEdit));
		return completionItem;
	}

	private CompletionItem createGroupCompletion(String group, String description, TextEdit textEdit) {
		CompletionItem completionItem = new CompletionItem(group);
		completionItem.setKind(CompletionItemKind.Module);
		if (description != null) {
			completionItem.setDocumentation(description);
		}
		completionItem.setTextEdit(Either.forLeft(textEdit));
		return completionItem;
	}

	private static int indexAfterDots(String key, long dots) {
		int index = 0;
		for (long i = 0; i < dots; i++) {
			index = key.indexOf('.', index) + 1;
		}
		return index;
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
			MainModel mainModel = catalog.mainModel();
			List<CompletionItem> allCompletionItems = new ArrayList<>();
			allCompletionItems.addAll(createGroupCompletionFromMainModel(mainModel, indexOfSecondDot));
			allCompletionItems.add(createCompletionItemForCamelComponent(indexOfSecondDot));
			return allCompletionItems.stream().filter(FilterPredicateUtils.matchesCompletionFilter(filterString)).collect(Collectors.toList());
		});
	}

	private CompletionItem createCompletionItemForCamelComponent(int indexOfSecondDot) {
		CompletionItem completionItem = new CompletionItem("component");
		completionItem.setDocumentation("Camel Component configurations");
		String insertText = indexOfSecondDot != -1 ? "component" : "component.";
		completionItem.setInsertText(insertText);
		Position start = new Position(getLine(), getStartPositionInLine() + CAMEL_KEY_PREFIX.length());
		Position end = new Position(getLine(), indexOfSecondDot != -1 ? getStartPositionInLine() + indexOfSecondDot : getEndPositionInLine());
		Range range = new Range(start, end);
		TextEdit textEdit = new TextEdit(range, insertText);
		completionItem.setTextEdit(Either.forLeft(textEdit));
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
			completionItem.setTextEdit(Either.forLeft(new TextEdit(range, insertText)));
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

	public CompletableFuture<Hover> getHover(Position position, CompletableFuture<CamelCatalog> camelCatalog, boolean markdown) {
		return getCatalogHover(position, camelCatalog).thenCombine(camelCatalog,
				(hover, catalog) -> hover != null ? hover : getRuntimeHover(RuntimeConfigurationProperties.of(catalog), markdown));
	}

	private Hover getRuntimeHover(RuntimeConfigurationProperties runtimeProperties, boolean markdown) {
		String key = camelPropertyKey.trim();
		return runtimeProperties.find(key).map(property -> {
			Hover hover = new Hover(ModelDocumentationHelper.getConfigurationPropertyDocumentation(property, markdown));
			int keyStart = getEndPositionInLine() - camelPropertyKey.stripLeading().length();
			hover.setRange(new Range(new Position(getLine(), keyStart), new Position(getLine(), keyStart + key.length())));
			return hover;
		}).orElse(null);
	}

	private CompletableFuture<Hover> getCatalogHover(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(camelComponentPropertyKey != null && camelComponentPropertyKey.isInRange(position.getCharacter())) {
			return camelComponentPropertyKey.getHover(position, camelCatalog);
		} else if(propertyGroup != null && propertyGroup.isInRange(position.getCharacter())) {
			return propertyGroup.getHover(position, camelCatalog);
		}
		return CompletableFuture.completedFuture(null);
	}

	public boolean shouldUseDashedCase() {
		return camelPropertyEntryInstance.shouldUseDashedCase();
	}

	public Collection<Diagnostic> validate(Set<CamelPropertyEntryInstance> allCamelPropertyEntriesOfTheFile) {
		Collection<Diagnostic> diagnostics = new HashSet<>();
		if(camelPropertyKey != null && camelPropertyKey.startsWith(CAMEL_KEY_PREFIX)) {
			Optional<String> duplicateByDashCamelNotationDifference = allCamelPropertyEntriesOfTheFile.stream()
				.map(CamelPropertyEntryInstance::getCamelPropertyKeyInstance)
				.filter(Objects::nonNull)
				.map(CamelPropertyKeyInstance::getCamelPropertyKey)
				.filter(Objects::nonNull)
				.filter(iterCamelPropertyKey ->
					!iterCamelPropertyKey.equals(camelPropertyKey)
					&& StringUtils.dashToCamelCase(iterCamelPropertyKey).equals(StringUtils.dashToCamelCase(camelPropertyKey)))
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
