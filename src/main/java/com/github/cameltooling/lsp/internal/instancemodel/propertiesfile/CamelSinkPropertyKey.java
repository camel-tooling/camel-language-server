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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.kafkaconnector.catalog.CamelKafkaConnectorCatalog;
import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

/**
 * Represents the subpart of the key after camel.sink.
 * For instance, with "camel.sink.timer.delay=1000",
 * it is used to represents "timer.delay"
 * 
 */
public class CamelSinkPropertyKey implements ILineRangeDefineable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelSinkPropertyKey.class);

	private static CamelKafkaConnectorCatalog catalog = new CamelKafkaConnectorCatalog();
	
	private String sinkParameter;
	private CamelPropertyKeyInstance camelPropertyKeyInstance;
	private String connectorClass;

	public CamelSinkPropertyKey(String sinkParameter, CamelPropertyKeyInstance camelPropertyKeyInstance, TextDocumentItem textDocumentItem) {
		this.sinkParameter = sinkParameter;
		this.camelPropertyKeyInstance = camelPropertyKeyInstance;
		this.connectorClass = findConnectorClass(textDocumentItem);
	}

	private String findConnectorClass(TextDocumentItem textDocumentItem) {
		Properties properties = new Properties();
		try {
			properties.load(new ByteArrayInputStream(textDocumentItem.getText().getBytes()));
			Object connectorClassValue = properties.get("connector.class");
			if (connectorClassValue != null) {
				return connectorClassValue.toString();
			}
		} catch (IOException e) {
			LOGGER.error("Cannot load Properties file to search for 'connector.class' property value.", e);
		}
		return null;
	}

	@Override
	public int getLine() {
		return camelPropertyKeyInstance.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return camelPropertyKeyInstance.getStartPositionInLine() + CamelPropertyKeyInstance.CAMEL_SINK_KEY_PREFIX.length();
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + sinkParameter.length();
	}
	
	public boolean isInRange(int positionChar) {
		return getStartPositionInLine() <= positionChar
				&& positionChar <= sinkParameter.length() + getStartPositionInLine();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		if (connectorClass != null) {
			Optional<CamelKafkaConnectorModel> camelKafkaConnectorModel = findConnectorModel();
			if (camelKafkaConnectorModel.isPresent()) {
				String filterString = sinkParameter.substring(0, position.getCharacter() - getStartPositionInLine());
				List<CompletionItem> completions = camelKafkaConnectorModel.get()
						.getOptions()
						.stream()
						.filter(option -> option.getName().startsWith(CamelPropertyKeyInstance.CAMEL_SINK_KEY_PREFIX))
						.map(option -> {
							CompletionItem completionItem = new CompletionItem(option.getName().replace(CamelPropertyKeyInstance.CAMEL_SINK_KEY_PREFIX, ""));
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

	private Optional<CamelKafkaConnectorModel> findConnectorModel() {
		return catalog.getConnectorsModel()
				.values()
				.stream()
				.filter(connector -> connectorClass.equals(connector.getConnectorClass()))
				.findAny();
	}

}
