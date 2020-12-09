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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorOptionModel;
import org.apache.camel.util.StringHelper;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;
import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;
import com.github.cameltooling.lsp.internal.parser.CamelKafkaUtil;

/**
 * Represents the subpart of the key after camel.sink. or camel.source.
 * For instance, with "camel.sink.timer.delay=1000",
 * it is used to represents "timer.delay"
 * 
 */
public class CamelSinkOrSourcePropertyKey implements ILineRangeDefineable {

	private String optionKey;
	private CamelPropertyKeyInstance camelPropertyKeyInstance;
	private String connectorClass;
	private String prefix;

	public CamelSinkOrSourcePropertyKey(String optionKey, CamelPropertyKeyInstance camelPropertyKeyInstance, TextDocumentItem textDocumentItem, String prefix) {
		this.optionKey = optionKey;
		this.camelPropertyKeyInstance = camelPropertyKeyInstance;
		this.connectorClass = new CamelKafkaUtil().findConnectorClass(textDocumentItem);
		this.prefix = prefix;
	}

	@Override
	public int getLine() {
		return camelPropertyKeyInstance.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return camelPropertyKeyInstance.getStartPositionInLine() + getPrefix().length();
	}

	private String getPrefix() {
		return prefix;
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + optionKey.length();
	}
	
	public boolean isInRange(int positionChar) {
		return getStartPositionInLine() <= positionChar
				&& positionChar <= optionKey.length() + getStartPositionInLine();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		if (connectorClass != null) {
			boolean shouldUseDashed = camelPropertyKeyInstance.shouldUseDashedCase();
			Optional<CamelKafkaConnectorModel> camelKafkaConnectorModel = camelKafkaConnectorManager.findConnectorModel(connectorClass);
			if (camelKafkaConnectorModel.isPresent()) {
				String filterString = optionKey.substring(0, position.getCharacter() - getStartPositionInLine());
				List<CompletionItem> completions = camelKafkaConnectorModel.get()
						.getOptions()
						.stream()
						.filter(option -> option.getName().startsWith(getPrefix()))
						.map(option -> {
							String realOptionName = option.getName().replace(getPrefix(), "");
							if(shouldUseDashed) {
								realOptionName = StringHelper.camelCaseToDash(realOptionName);
							}
							CompletionItem completionItem = new CompletionItem(realOptionName);
							CompletionResolverUtils.applyTextEditToCompletionItem(this, completionItem);
							completionItem.setDocumentation(option.getDescription());
							return completionItem;
						})
						.filter(FilterPredicateUtils.matchesCompletionFilter(filterString))
						.collect(Collectors.toList());
				return CompletableFuture.completedFuture(completions);
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}



	public CompletableFuture<Hover> getHover(CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		if (connectorClass != null) {
			Optional<CamelKafkaConnectorModel> camelKafkaConnectorModel = camelKafkaConnectorManager.findConnectorModel(connectorClass);
			if (camelKafkaConnectorModel.isPresent()) {
				String propertyKey = getPrefix() + optionKey;
				String camelCasePropertyKey = StringHelper.dashToCamelCase(propertyKey);
				Hover hover = camelKafkaConnectorModel.get()
						.getOptions()
						.stream()
						.filter(option -> camelCasePropertyKey.equals(option.getName()))
						.map(option -> createHover(option.getDescription()))
						.findAny().orElse(null);
				return CompletableFuture.completedFuture(hover);
			}
		}
		return CompletableFuture.completedFuture(null);
	}

	public String getOptionKey() {
		return optionKey;
	}

	public Collection<Diagnostic> validate(CamelKafkaConnectorCatalogManager camelKafkaConnectorManager, Set<CamelPropertyEntryInstance> allCamelPropertyEntriesOfTheFile) {
		Set<Diagnostic> diagnostics = new HashSet<>();
		Optional<CamelKafkaConnectorModel> connectorModelOptional = camelKafkaConnectorManager.findConnectorModel(connectorClass);
		if (connectorModelOptional.isPresent()) {
			CamelKafkaConnectorModel connectorModel = connectorModelOptional.get();
			diagnostics.addAll(validateExistingProperty(connectorModel));
			diagnostics.addAll(validateSourceSinkMatch(connectorModel));
			diagnostics.addAll(validateSinkSourceMatch(connectorModel));
			diagnostics.addAll(validateUrlNotMixedWithListOfProperties(connectorModel, allCamelPropertyEntriesOfTheFile));
		}
		return diagnostics;
	}

	private Collection<Diagnostic> validateUrlNotMixedWithListOfProperties(CamelKafkaConnectorModel connectorModel, Set<CamelPropertyEntryInstance> allCamelPropertyEntriesOfTheFile) {
		if("url".equals(optionKey)) {
			Set<String> modelProperties = connectorModel
					.getOptions()
					.stream()
					.map(CamelKafkaConnectorOptionModel::getName)
					.filter(propertyName -> propertyName.startsWith(prefix))
					.collect(Collectors.toSet());
			
			Set<String> camelComponentProperties = allCamelPropertyEntriesOfTheFile.stream()
									.map(property -> property.getCamelPropertyKeyInstance().getCamelPropertyKey())
									.filter(propertyKey -> modelProperties.contains(StringHelper.dashToCamelCase(propertyKey)))
									.collect(Collectors.toSet());
			if(!camelComponentProperties.isEmpty()) {
				return Collections.singleton(new Diagnostic(
						new Range(new Position(camelPropertyKeyInstance.getLine(), camelPropertyKeyInstance.getStartPositionInLine()), new Position(camelPropertyKeyInstance.getLine(), camelPropertyKeyInstance.getEndPositionInLine())),
						"Camel URL cannot be used when the component is configured through a list of Properties. Properties used: " + camelComponentProperties.stream().collect(Collectors.joining(",")),
						DiagnosticSeverity.Error,
						DiagnosticService.APACHE_CAMEL_VALIDATION));
			}
		}
		return Collections.emptySet();
	}

	private Collection<Diagnostic> validateSinkSourceMatch(CamelKafkaConnectorModel connectorModel) {
		return validateTypeMatch(connectorModel, "sink", "source");
	}

	private Collection<Diagnostic> validateSourceSinkMatch(CamelKafkaConnectorModel connectorModel) {
		return validateTypeMatch(connectorModel, "source", "sink");
	}
	
	private Collection<Diagnostic> validateTypeMatch(CamelKafkaConnectorModel connectorModel, String connectorClassTypeChecked, String propertyPrefixTypeChecked) {
		if(connectorClassTypeChecked.equals(connectorModel.getType()) && prefix.startsWith(CamelPropertyKeyInstance.CAMEL_KEY_PREFIX + propertyPrefixTypeChecked)) {
			return Collections.singleton(new Diagnostic(
				new Range(new Position(camelPropertyKeyInstance.getLine(), camelPropertyKeyInstance.getStartPositionInLine()), new Position(camelPropertyKeyInstance.getLine(), camelPropertyKeyInstance.getEndPositionInLine())),
				"`"+propertyPrefixTypeChecked+"` property used although the connector class is of type `"+connectorClassTypeChecked+"`: " + camelPropertyKeyInstance.getCamelPropertyKey(),
				DiagnosticSeverity.Error,
				DiagnosticService.APACHE_CAMEL_VALIDATION));
		}
		return Collections.emptySet();
	}

	private Set<Diagnostic> validateExistingProperty(CamelKafkaConnectorModel connectorModel) {
		if (!"url".equals(optionKey) && (optionKey.startsWith("endpoint") || optionKey.startsWith("path"))) {
			String propertyKey = getPrefix() + optionKey;
			String camelCasePropertyKey = StringHelper.dashToCamelCase(propertyKey);
			Optional<CamelKafkaConnectorOptionModel> optionModel = connectorModel
					.getOptions()
					.stream()
					.filter(option -> camelCasePropertyKey.equals(option.getName()))
					.findAny();
			if (!optionModel.isPresent()) {
				return Collections.singleton(new Diagnostic(
						new Range(new Position(getLine(), getStartPositionInLine()), new Position(getLine(), getEndPositionInLine())),
						"Unknown property " + optionKey,
						DiagnosticSeverity.Error,
						DiagnosticService.APACHE_CAMEL_VALIDATION));
			}
		}
		return Collections.emptySet();
	}

}
