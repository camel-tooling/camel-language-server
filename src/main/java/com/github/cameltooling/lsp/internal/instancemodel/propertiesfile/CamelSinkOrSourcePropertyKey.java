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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;
import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;
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
			Optional<CamelKafkaConnectorModel> camelKafkaConnectorModel = findConnectorModel(camelKafkaConnectorManager);
			if (camelKafkaConnectorModel.isPresent()) {
				String filterString = optionKey.substring(0, position.getCharacter() - getStartPositionInLine());
				List<CompletionItem> completions = camelKafkaConnectorModel.get()
						.getOptions()
						.stream()
						.filter(option -> option.getName().startsWith(getPrefix()))
						.map(option -> {
							CompletionItem completionItem = new CompletionItem(option.getName().replace(getPrefix(), ""));
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

	private Optional<CamelKafkaConnectorModel> findConnectorModel(CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		return camelKafkaConnectorManager.getCatalog().getConnectorsModel()
				.values()
				.stream()
				.filter(connector -> connectorClass.equals(connector.getConnectorClass()))
				.findAny();
	}

}
