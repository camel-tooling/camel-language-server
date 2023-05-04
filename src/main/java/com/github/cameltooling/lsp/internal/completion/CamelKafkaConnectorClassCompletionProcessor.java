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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyValueInstance;

@Deprecated
public class CamelKafkaConnectorClassCompletionProcessor {

	private CamelPropertyValueInstance camelPropertyValueInstance;
	private CamelKafkaConnectorCatalogManager camelKafkaConnectorManager;

	public CamelKafkaConnectorClassCompletionProcessor(CamelPropertyValueInstance camelPropertyValueInstance, CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		this.camelPropertyValueInstance = camelPropertyValueInstance;
		this.camelKafkaConnectorManager = camelKafkaConnectorManager;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(String startFilter) {
		Collection<CamelKafkaConnectorModel> camelKafkaConnectors = camelKafkaConnectorManager.getCatalog().getConnectorsModel().values();
		List<CompletionItem> completions = camelKafkaConnectors.stream()
				.map(camelKafkaConnector -> {
					String qualifiedConnectorClassName = camelKafkaConnector.getConnectorClass();
					CompletionItem completionItem = new CompletionItemCreator().createForQualifiedClassName(qualifiedConnectorClassName, camelPropertyValueInstance);
					completionItem.setDocumentation(camelKafkaConnector.getDescription());
					return completionItem;
				})
				.filter(FilterPredicateUtils.matchesCompletionFilter(startFilter))
				.collect(Collectors.toList());
		
		return CompletableFuture.completedFuture(completions);
	}
}
