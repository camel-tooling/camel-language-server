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
package com.github.cameltooling.lsp.internal.documentsymbol;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.parser.XmlRouteParser;
import org.apache.camel.parser.model.CamelEndpointDetails;
import org.apache.camel.parser.model.CamelNodeDetails;
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

public class DocumentSymbolXMLProcessor extends AbstractDocumentSymbolProcessor {
	
	static final String CANNOT_DETERMINE_DOCUMENT_SYMBOLS = "Cannot determine document symbols";
	private static final String ATTRIBUTE_ID = "id";
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSymbolXMLProcessor.class);
	
	private ParserXMLFileHelper parserFileHelper = new ParserXMLFileHelper();

	public DocumentSymbolXMLProcessor(TextDocumentItem textDocumentItem) {
		super(textDocumentItem);
	}

	public List<Either<SymbolInformation, DocumentSymbol>> getSymbolInformations() {
		List<Either<SymbolInformation, DocumentSymbol>> symbolInformations = new ArrayList<>();
		try {
			String rawpath = URI.create(textDocumentItem.getUri()).getRawPath();
			List<CamelNodeDetails> camelNodeDetails = XmlRouteParser.parseXmlRouteTree(createInputStream(textDocumentItem), "", rawpath);
			List<CamelEndpointDetails> endpoints = new ArrayList<>();
			XmlRouteParser.parseXmlRouteEndpoints(createInputStream(textDocumentItem), "", rawpath, endpoints);
			symbolInformations.addAll(createSymbolInformations(camelNodeDetails, endpoints));
			NodeList routeNodes = parserFileHelper.getRouteNodes(textDocumentItem);
			if (routeNodes != null) {
				symbolInformations.addAll(convertToSymbolInformation(routeNodes));
			}
			NodeList camelContextNodes = parserFileHelper.getCamelContextNodes(textDocumentItem);
			if (camelContextNodes != null) {
				symbolInformations.addAll(convertToSymbolInformation(camelContextNodes));
			}
		} catch (Exception e) {
			LOGGER.error(CANNOT_DETERMINE_DOCUMENT_SYMBOLS, e);
		}
		return symbolInformations;
	}

	private ByteArrayInputStream createInputStream(TextDocumentItem textDocumentItem) {
		return new ByteArrayInputStream(textDocumentItem.getText().getBytes());
	}
	
	private List<Either<SymbolInformation, DocumentSymbol>> convertToSymbolInformation(NodeList routeNodes) {
		List<Either<SymbolInformation, DocumentSymbol>> res = new ArrayList<>();
		for (int i = 0; i < routeNodes.getLength(); i++) {
			Node routeNode = routeNodes.item(i);
			Location location = parserFileHelper.retrieveLocation(routeNode, textDocumentItem);
			String displayNameOfSymbol = computeDisplayNameOfSymbol(routeNode);
			res.add(Either.forRight(new DocumentSymbol(displayNameOfSymbol, SymbolKind.Field, location.getRange(), location.getRange())));
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
