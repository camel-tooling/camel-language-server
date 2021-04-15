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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.CamelUriElementInstance;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelper;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperFactory;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

public class CamelEndpointCompletionProcessor {

	public static final String ERROR_SEARCHING_FOR_CORRESPONDING_NODE_ELEMENTS = "Error searching for corresponding node elements";
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelEndpointCompletionProcessor.class);
	private TextDocumentItem textDocumentItem;
	private CompletableFuture<CamelCatalog> camelCatalog;

	public CamelEndpointCompletionProcessor(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog) {
		this.textDocumentItem = textDocumentItem;
		this.camelCatalog = camelCatalog;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, SettingsManager settingsManager) {
		if (textDocumentItem != null) {
			try {
				ParserFileHelper parserFileHelper = new ParserFileHelperFactory().getCorrespondingParserFileHelper(textDocumentItem, position.getLine());
				if (parserFileHelper != null) {
					String camelComponentUri = parserFileHelper.getCamelComponentUri(textDocumentItem, position);
					if (camelComponentUri != null) {
						CamelURIInstance camelURIInstance = parserFileHelper.createCamelURIInstance(textDocumentItem, position, camelComponentUri);
						int positionInCamelUri = parserFileHelper.getPositionInCamelURI(textDocumentItem, position);
						return getCompletions(camelURIInstance, positionInCamelUri, settingsManager);
					}
				}
			} catch (Exception e) {
				LOGGER.error(ERROR_SEARCHING_FOR_CORRESPONDING_NODE_ELEMENTS, e);
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	private CompletableFuture<List<CompletionItem>> getCompletions(CamelURIInstance camelURIInstance, int positionInCamelUri, SettingsManager settingsManager) {
		CamelUriElementInstance camelUriElementInstance = camelURIInstance.getSpecificElement(positionInCamelUri);
		return camelUriElementInstance.getCompletions(camelCatalog, positionInCamelUri, textDocumentItem, settingsManager);
	}

}
