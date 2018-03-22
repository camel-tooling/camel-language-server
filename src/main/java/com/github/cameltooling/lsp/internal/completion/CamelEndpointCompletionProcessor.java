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
import org.w3c.dom.Node;

import com.github.cameltooling.lsp.internal.parser.CamelURIInstance;
import com.github.cameltooling.lsp.internal.parser.CamelUriElementInstance;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelper;

public class CamelEndpointCompletionProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(CamelEndpointCompletionProcessor.class);
	private TextDocumentItem textDocumentItem;
	private ParserFileHelper parserFileHelper = new ParserFileHelper();
	private CompletableFuture<CamelCatalog> camelCatalog;

	public CamelEndpointCompletionProcessor(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog) {
		this.textDocumentItem = textDocumentItem;
		this.camelCatalog = camelCatalog;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		if(textDocumentItem != null) {
			try {
				Node correspondingCamelNode = parserFileHelper.getCorrespondingCamelNodeForCompletion(textDocumentItem, position.getLine());
				if (correspondingCamelNode != null) {
					String line = parserFileHelper.getLine(textDocumentItem, position);
					String camelComponentUri = parserFileHelper.getCamelComponentUri(textDocumentItem, position);
					CamelURIInstance camelURIInstance = new CamelURIInstance(camelComponentUri, correspondingCamelNode);
					int positionInCamelUri = position.getCharacter() - line.indexOf("uri=") - 5;
					CamelUriElementInstance camelUriElementInstance = camelURIInstance.getSpecificElement(positionInCamelUri);
					return camelUriElementInstance.getCompletions(camelCatalog, positionInCamelUri);
				}
			} catch (Exception e) {
				LOGGER.error("Error searching for corresponding node elements", e);
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

}
