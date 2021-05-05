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
package com.github.cameltooling.lsp.internal.hover;

import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.catalog.util.StringUtils;
import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.CamelUriElementInstance;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelper;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperFactory;

public class CamelURIHoverProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelURIHoverProcessor.class);
	private TextDocumentItem textDocumentItem;
	private CompletableFuture<CamelCatalog> camelCatalog;
	private KameletsCatalogManager kameletCatalogManager;

	public CamelURIHoverProcessor(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog, KameletsCatalogManager kameletCatalogManager) {
		this.textDocumentItem = textDocumentItem;
		this.camelCatalog = camelCatalog;
		this.kameletCatalogManager = kameletCatalogManager;
	}

	public CompletableFuture<Hover> getHover(Position position) {
		try {
			ParserFileHelper parserFileHelper = new ParserFileHelperFactory().getCorrespondingParserFileHelper(textDocumentItem, position.getLine());
			if (parserFileHelper != null){
				String camelComponentUri = parserFileHelper.getCamelComponentUri(textDocumentItem, position);
				String componentName = StringUtils.asComponentName(camelComponentUri);
				if (componentName != null) {
					CamelURIInstance camelURIInstance = parserFileHelper.createCamelURIInstance(textDocumentItem, position, camelComponentUri);
					int positionInCamelUri = parserFileHelper.getPositionInCamelURI(textDocumentItem, position);
					CamelUriElementInstance elem = camelURIInstance.getSpecificElement(positionInCamelUri);
					return camelCatalog.thenApply(new CamelURIHoverFuture(elem, kameletCatalogManager));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error searching hover", e);
		}
		return CompletableFuture.completedFuture(null);
	}

}
