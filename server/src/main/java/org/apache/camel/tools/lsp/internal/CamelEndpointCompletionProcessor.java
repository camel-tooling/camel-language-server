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
package org.apache.camel.tools.lsp.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CamelEndpointCompletionProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelEndpointCompletionProcessor.class);
	private static final String NAMESPACEURI_CAMEL_BLUEPRINT = "http://camel.apache.org/schema/blueprint";
	private static final String NAMESPACEURI_CAMEL_SPRING = "http://camel.apache.org/schema/spring";
	private TextDocumentItem textDocumentItem;

	public CamelEndpointCompletionProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}

	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletions() {
		if(textDocumentItem != null) {
			try {
				if(getCorrespondingCamelNodeForCompletion(textDocumentItem) != null) {
					return CompletableFuture.completedFuture(Either.forLeft(Arrays.asList(new CompletionItem("dummyCamelCompletion"))));
				}
			} catch (Exception e) {
				LOGGER.error("Error searching for corresponding node elements", e);
			}
		}
		return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
	}
	
	/**
	 * @param textDocumentItem
	 * @return Currently returns the first from Camel Node ignoring the exact position
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	private Node getCorrespondingCamelNodeForCompletion(TextDocumentItem textDocumentItem) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document xmlParsed = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(textDocumentItem.getText().getBytes(StandardCharsets.UTF_8)));
		NodeList childNodes = xmlParsed.getElementsByTagName("from");
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (NAMESPACEURI_CAMEL_BLUEPRINT.equals(child.getNamespaceURI()) || NAMESPACEURI_CAMEL_SPRING.equals(child.getNamespaceURI())) {
				return child;
			}
		}
		return null;
	}

}
