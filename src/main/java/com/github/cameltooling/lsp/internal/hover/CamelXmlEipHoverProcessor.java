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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.parser.helper.XmlLineNumberParser;
import org.apache.camel.tooling.model.EipModel;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.github.cameltooling.lsp.internal.catalog.util.ModelDocumentationHelper;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;
import com.github.cameltooling.lsp.internal.parser.ParserXMLFileHelper;

/**
 * Hover on the name of an element of a Camel XML DSL route, documenting the EIP, expression language or data format.
 */
public class CamelXmlEipHoverProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(CamelXmlEipHoverProcessor.class);

	private final TextDocumentItem textDocumentItem;
	private final CompletableFuture<CamelCatalog> camelCatalog;
	private final boolean markdown;

	public CamelXmlEipHoverProcessor(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog, boolean markdown) {
		this.textDocumentItem = textDocumentItem;
		this.camelCatalog = camelCatalog;
		this.markdown = markdown;
	}

	/**
	 * @return the hover, or null when the position is not on the name of an element of a Camel namespace
	 */
	public CompletableFuture<Hover> getHover(Position position) {
		String line = new ParserFileHelperUtil().getLine(textDocumentItem, position.getLine());
		int column = position.getCharacter();
		if (line == null || column >= line.length() || !isNameCharacter(line.charAt(column))) {
			return null;
		}
		int start = column;
		while (start > 0 && isNameCharacter(line.charAt(start - 1))) {
			start--;
		}
		int end = column;
		while (end < line.length() && isNameCharacter(line.charAt(end))) {
			end++;
		}
		boolean endTag = start >= 2 && line.charAt(start - 1) == '/' && line.charAt(start - 2) == '<';
		boolean startTag = start >= 1 && line.charAt(start - 1) == '<';
		if (!startTag && !endTag) {
			return null;
		}
		String qualifiedName = line.substring(start, end);
		String localName = qualifiedName.substring(qualifiedName.lastIndexOf(':') + 1);
		if (!isCamelElement(localName, position.getLine(), endTag)) {
			return null;
		}
		Range range = new Range(new Position(position.getLine(), start), new Position(position.getLine(), end));
		return camelCatalog.thenApply(catalog -> {
			EipModel model = catalog.eipModel(localName);
			return model != null ? new Hover(ModelDocumentationHelper.getModelDocumentation(model, markdown), range) : null;
		});
	}

	private boolean isCamelElement(String localName, int line, boolean endTag) {
		try {
			Document document = XmlLineNumberParser.parseXml(new ByteArrayInputStream(textDocumentItem.getText().getBytes(StandardCharsets.UTF_8)));
			NodeList elements = document.getElementsByTagNameNS("*", localName);
			// the line numbers of the parser start at 1
			String expectedLine = String.valueOf(line + 1);
			for (int i = 0; i < elements.getLength(); i++) {
				Node element = elements.item(i);
				Object elementLine = element.getUserData(endTag ? XmlLineNumberParser.LINE_NUMBER_END : XmlLineNumberParser.LINE_NUMBER);
				if (expectedLine.equals(elementLine) && ParserXMLFileHelper.isCamelNamespace(element.getNamespaceURI())) {
					return true;
				}
			}
		} catch (Exception e) {
			LOGGER.debug("Cannot parse the XML document to provide hover on element names", e);
		}
		return false;
	}

	private static boolean isNameCharacter(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.' || c == ':';
	}
}
