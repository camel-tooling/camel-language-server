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
package com.github.cameltooling.lsp.internal.documentsymbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.cameltooling.lsp.internal.parser.ParserXMLFileHelper;

public class DocumentSymbolProcessor {
	
	private static final String ATTRIBUTE_ID = "id";
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSymbolProcessor.class);
	private TextDocumentItem textDocumentItem;
	private ParserXMLFileHelper parserFileHelper = new ParserXMLFileHelper();

	public DocumentSymbolProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}
	
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> getDocumentSymbols() {
		return CompletableFuture.supplyAsync(() -> {
			try {
				List<Either<SymbolInformation, DocumentSymbol>> symbolInformations = new ArrayList<>();
				NodeList routeNodes = parserFileHelper.getRouteNodes(textDocumentItem);
				if (routeNodes != null) {
					symbolInformations.addAll(convertToSymbolInformation(routeNodes));
				}
				NodeList camelContextNodes = parserFileHelper.getCamelContextNodes(textDocumentItem);
				if (camelContextNodes != null) {
					symbolInformations.addAll(convertToSymbolInformation(camelContextNodes));
				}
				return symbolInformations;
			} catch (Exception e) {
				LOGGER.error("Cannot determine document symbols", e);
			}
			return Collections.emptyList();
		});
	}

	private List<Either<SymbolInformation, DocumentSymbol>> convertToSymbolInformation(NodeList routeNodes) {
		List<Either<SymbolInformation, DocumentSymbol>> res = new ArrayList<>();
		for (int i = 0; i < routeNodes.getLength(); i++) {
			Node routeNode = routeNodes.item(i);
			Location location = parserFileHelper.retrieveLocation(routeNode, textDocumentItem);
			String displayNameOfSymbol = computeDisplayNameOfSymbol(routeNode);
			res.add(Either.forLeft(new SymbolInformation(displayNameOfSymbol, SymbolKind.Field, location)));
		}
		return res;
	}

	private String computeDisplayNameOfSymbol(Node node) {
		Node routeIdAttribute = node.getAttributes().getNamedItem(ATTRIBUTE_ID);
		String displayNameOfSymbol;
		if (routeIdAttribute != null) {
			displayNameOfSymbol = routeIdAttribute.getNodeValue();
		} else {
			displayNameOfSymbol = "<no id>";
		}
		return displayNameOfSymbol;
	}
}
