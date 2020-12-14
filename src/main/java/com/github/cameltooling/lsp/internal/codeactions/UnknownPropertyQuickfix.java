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
package com.github.cameltooling.lsp.internal.codeactions;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorOptionModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.CamelTextDocumentService;
import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;
import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyKeyInstance;
import com.github.cameltooling.lsp.internal.parser.CamelKafkaUtil;

public class UnknownPropertyQuickfix extends AbstractQuickfix {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UnknownPropertyQuickfix.class);

	public UnknownPropertyQuickfix(CamelTextDocumentService camelTextDocumentService) {
		super(camelTextDocumentService);
	}

	protected String getDiagnosticId() {
		return DiagnosticService.ERROR_CODE_UNKNOWN_PROPERTIES;
	}

	protected List<String> retrievePossibleValues(TextDocumentItem textDocumentItem,
			CompletableFuture<CamelCatalog> camelCatalog,
			CamelKafkaConnectorCatalogManager camelKafkaConnectorManager,
			Position position) {
		if (textDocumentItem.getUri().endsWith(".properties")) {
			Optional<CamelKafkaConnectorModel> optionalModel = camelKafkaConnectorManager.findConnectorModel(new CamelKafkaUtil().findConnectorClass(textDocumentItem));
			if (optionalModel.isPresent()) {
				return optionalModel.get().getOptions()
						.stream()
						.map(CamelKafkaConnectorOptionModel::getName)
						.map(this::removePrefix)
						.collect(Collectors.toList());
			}
		} else {
			try {
				return new CamelEndpointCompletionProcessor(textDocumentItem, camelCatalog).getCompletions(position)
						.thenApply(completionItems -> completionItems.stream().map(CompletionItem::getInsertText)
								.collect(Collectors.toList()))
						.get();
			} catch (InterruptedException e) {
				LOGGER.error("Interruption while computing possible properties for quickfix", e);
				Thread.currentThread().interrupt();
				return Collections.emptyList();
			} catch (ExecutionException e) {
				LOGGER.error("Exception while computing possible properties for quickfix", e);
				return Collections.emptyList();
			}
		}
		return Collections.emptyList();
	}

	private String removePrefix(String value) {
		return value
				.replace(CamelPropertyKeyInstance.CAMEL_SINK_KEY_PREFIX, "")
				.replace(CamelPropertyKeyInstance.CAMEL_SOURCE_KEY_PREFIX, "");
	}

}
