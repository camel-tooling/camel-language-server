/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.instancemodel.propertiesfile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.RuntimeConfigurationProperties;
import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.RuntimeConfigurationProperty;
import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.completion.CamelComponentOptionValuesCompletionsFuture;
import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

/**
 * Represents one value in properties file.
 * For instance, with "camel.component.timer.delay=1000",
 * it is used to represents "1000"
 */
public class CamelPropertyValueInstance implements ILineRangeDefineable {

	private String camelPropertyValue;
	private CamelPropertyKeyInstance key;

	public CamelPropertyValueInstance(String camelPropertyFileValue, CamelPropertyKeyInstance key, TextDocumentItem textDocumentItem) {
		this.camelPropertyValue = camelPropertyFileValue;
		this.key = key;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog, KameletsCatalogManager kameletsCatalogManager) {
		String startFilter = computeStartFilter(position);
		return camelCatalog.thenApply(catalog -> {
			List<CompletionItem> completions = new CamelComponentOptionValuesCompletionsFuture(this, startFilter).apply(catalog);
			return completions.isEmpty() ? getRuntimeConfigurationPropertyValues(RuntimeConfigurationProperties.of(catalog), startFilter) : completions;
		});
	}

	/**
	 * @return the possible values of the configuration property of the runtime with this key, when it is an enumeration or a boolean
	 */
	private List<CompletionItem> getRuntimeConfigurationPropertyValues(RuntimeConfigurationProperties runtimeProperties, String startFilter) {
		Optional<RuntimeConfigurationProperty> property = runtimeProperties.find(key.getCamelPropertyKey().trim());
		if (property.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> values = property.get().isBoolean() ? Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()) : property.get().getEnumValues();
		// the spaces after the separator are kept
		int valueStart = getEndPositionInLine() - camelPropertyValue.stripLeading().length();
		Range range = new Range(new Position(getLine(), valueStart), new Position(getLine(), getEndPositionInLine()));
		return values.stream()
				.map(value -> {
					CompletionItem completionItem = new CompletionItem(value);
					completionItem.setTextEdit(Either.forLeft(new TextEdit(range, value)));
					return completionItem;
				})
				.filter(FilterPredicateUtils.matchesCompletionFilter(startFilter.stripLeading()))
				.collect(Collectors.toList());
	}

	private String computeStartFilter(Position position) {
		return camelPropertyValue.substring(0, position.getCharacter() - key.getEndposition() -1);
	}

	public String getCamelPropertyFileValue() {
		return camelPropertyValue;
	}
	
	public CamelPropertyKeyInstance getKey() {
		return key;
	}

	@Override
	public int getLine() {
		return key.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return key.getEndposition() + 1;
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + (camelPropertyValue != null ? camelPropertyValue.length() : 0);
	}

	public CompletableFuture<Hover> getHover(Position position, CompletableFuture<CamelCatalog> camelCatalog, KameletsCatalogManager kameletCatalogManager) {
		return CompletableFuture.completedFuture(null);
	}

}
