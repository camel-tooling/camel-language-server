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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
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
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;
import com.github.cameltooling.lsp.internal.parser.CamelKafkaUtil;

/**
 * Represents one entry in properties file or in Camel K modeline. For instance, the whole entry "camel.component.timer.delay=1000"
 *
 */
public class CamelPropertyEntryInstance implements ILineRangeDefineable {
	
	private CamelPropertyKeyInstance camelPropertyKeyInstance;
	private CamelPropertyValueInstance camelPropertyValueInstance;
	private String line;
	private Position startPosition;
	private TextDocumentItem textDocumentItem;

	public CamelPropertyEntryInstance(String line, Position startPosition, TextDocumentItem textDocumentItem) {
		this.line = line;
		this.startPosition = startPosition;
		this.textDocumentItem = textDocumentItem;
		int indexOf = line.indexOf('=');
		String camelPropertyFileKeyInstanceString;
		String camelPropertyFileValueInstanceString;
		if (indexOf != -1) {
			camelPropertyFileKeyInstanceString = line.substring(0, indexOf);
			camelPropertyFileValueInstanceString = line.substring(indexOf+1);
		} else {
			camelPropertyFileKeyInstanceString = line;
			camelPropertyFileValueInstanceString = null;
		}
		camelPropertyKeyInstance = new CamelPropertyKeyInstance(camelPropertyFileKeyInstanceString, this, textDocumentItem);
		camelPropertyValueInstance = new CamelPropertyValueInstance(camelPropertyFileValueInstanceString, camelPropertyKeyInstance, textDocumentItem);
	}
	
	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog, CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		if (isOnPropertyKey(position)) {
			return camelPropertyKeyInstance.getCompletions(position, camelCatalog, camelKafkaConnectorManager);
		} else {
			return camelPropertyValueInstance.getCompletions(position, camelCatalog, camelKafkaConnectorManager);
		}
	}

	private boolean isOnPropertyKey(Position position) {
		return position.getCharacter() <= camelPropertyKeyInstance.getEndposition();
	}
	
	CamelPropertyKeyInstance getCamelPropertyKeyInstance() {
		return camelPropertyKeyInstance;
	}

	CamelPropertyValueInstance getCamelPropertyValueInstance() {
		return camelPropertyValueInstance;
	}

	public int getLine() {
		return startPosition.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition.getCharacter();
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + line.length();
	}

	public CompletableFuture<Hover> getHover(Position position, CompletableFuture<CamelCatalog> camelCatalog, CamelKafkaConnectorCatalogManager camelKafkaConnectorCatalog) {
		if (isOnPropertyKey(position)) {
			return camelPropertyKeyInstance.getHover(position, camelCatalog, camelKafkaConnectorCatalog);
		} else {
			return camelPropertyValueInstance.getHover(position, camelCatalog);
		}
	}

	public boolean shouldUseDashedCase() {
		return textDocumentItem != null
				&& new DashedCaseDetector().hasDashedCaseInCamelPropertyOption(textDocumentItem.getText());
	}

	public Collection<Diagnostic> validate(CamelKafkaConnectorCatalogManager camelKafkaConnectorManager, Set<CamelPropertyEntryInstance> allCamelPropertyEntriesOfTheFile) {
		Collection<Diagnostic> diagnostics = new HashSet<>();
		if (camelPropertyKeyInstance != null) {
			diagnostics.addAll(camelPropertyKeyInstance.validate(camelKafkaConnectorManager, allCamelPropertyEntriesOfTheFile));
			diagnostics.addAll(validateRequiredPropertiesWithoutDefaultValues(camelKafkaConnectorManager, allCamelPropertyEntriesOfTheFile));
		}
		return diagnostics;
	}

	private Collection<? extends Diagnostic> validateRequiredPropertiesWithoutDefaultValues(
			CamelKafkaConnectorCatalogManager camelKafkaConnectorManager,
			Set<CamelPropertyEntryInstance> allCamelPropertyEntriesOfTheFile) {
		Collection<Diagnostic> diagnostics = new HashSet<>();
		CamelKafkaUtil camelKafkaUtil = new CamelKafkaUtil();
		if (camelPropertyKeyInstance != null && camelKafkaUtil.isConnectorClassForCamelKafkaConnector(camelPropertyKeyInstance.getCamelPropertyKey())) {
			Optional<CamelKafkaConnectorModel> model = camelKafkaUtil.findConnectorModel(textDocumentItem, camelKafkaConnectorManager);
			if (model.isPresent()) {
				Set<String> allPropertykeys = findAllKeys(allCamelPropertyEntriesOfTheFile);
				if (!(allPropertykeys.contains("camel.sink.url") || allPropertykeys.contains("camel.source.url"))) {
					Set<String> missingMandatoryProperties = findMissingProperties(model.get(), allPropertykeys);
					if (!missingMandatoryProperties.isEmpty()) {
						diagnostics.add(new Diagnostic(
								new Range(new Position(getLine(), getStartPositionInLine()),new Position(getLine(), getEndPositionInLine())),
								"Some required properties without default values are missing. Properties missing: " + missingMandatoryProperties.stream().collect(Collectors.joining(",")),
								DiagnosticSeverity.Error,
								DiagnosticService.APACHE_CAMEL_VALIDATION));
					}
				}
			}
		}
		return diagnostics;
	}

	private Set<String> findAllKeys(Set<CamelPropertyEntryInstance> allCamelPropertyEntriesOfTheFile) {
		return allCamelPropertyEntriesOfTheFile.stream()
				.map(CamelPropertyEntryInstance::getCamelPropertyKeyInstance).filter(Objects::nonNull)
				.map(CamelPropertyKeyInstance::getCamelPropertyKey).filter(Objects::nonNull)
				.map(StringHelper::dashToCamelCase)
				.collect(Collectors.toSet());
	}

	private Set<String> findMissingProperties(CamelKafkaConnectorModel camelKafkaConnectorModel, Set<String> allPropertykeys) {
		return camelKafkaConnectorModel.getOptions().stream()
				.filter(option -> "true".equals(option.getRequired()) && option.getDefaultValue() == null)
				.map(CamelKafkaConnectorOptionModel::getName)
				.map(StringHelper::dashToCamelCase)
				.filter(optionName -> !allPropertykeys.contains(optionName))
				.collect(Collectors.toSet());
	}

}
