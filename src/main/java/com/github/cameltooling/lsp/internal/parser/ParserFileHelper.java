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
package com.github.cameltooling.lsp.internal.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.camel.parser.helper.XmlLineNumberParser;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ParserFileHelper {
	
	private static final String NAMESPACEURI_CAMEL_BLUEPRINT = "http://camel.apache.org/schema/blueprint";
	private static final String NAMESPACEURI_CAMEL_SPRING = "http://camel.apache.org/schema/spring";
	private static final List<String> CAMEL_NODES_TAG = Arrays.asList("to", "from");
	
	public String getLine(TextDocumentItem textDocumentItem, Position position) {
		String text = textDocumentItem.getText();
		String[] lines = text.split("\\r?\\n", position.getLine() + 2);
		if (lines.length >= position.getLine() + 1) {
			return lines[position.getLine()];
		}
		return null;
	}
	
	public String getCamelComponentUri(String line, int characterPosition) {
		int uriAttribute = line.indexOf("uri=\"");
		if(uriAttribute != -1) {
			int nextQuote = line.indexOf('\"', uriAttribute + 5);
			if (isBetween(characterPosition, uriAttribute + 5, nextQuote)) {
				return line.substring(uriAttribute + 5, nextQuote);
			}
		}
		return null;
	}
	
	public String getCamelComponentUri(TextDocumentItem textDocumentItem, Position position) {
		return getCamelComponentUri(getLine(textDocumentItem, position), position.getCharacter());
	}
	
	private boolean isBetween(int position, int start, int end) {
		return end != -1 && position <= end && position >= start;
	}
	
	/**
	 * @param textDocumentItem	the text document item
	 * @param line 	the line number
	 * @return Currently returns the first from Camel Node ignoring the exact position
	 * @throws Exception on any error 
	 */
	public Node getCorrespondingCamelNodeForCompletion(TextDocumentItem textDocumentItem, int line) throws Exception {
		if (hasElementFromCamelNamespace(textDocumentItem)) {
			Document parseXml = XmlLineNumberParser.parseXml(new ByteArrayInputStream(textDocumentItem.getText().getBytes(StandardCharsets.UTF_8)));
			Element documentElement = parseXml.getDocumentElement();
			return findElementAtLine(line, documentElement);
		} else {
			return null;
		}
	}

	private boolean hasElementFromCamelNamespace(TextDocumentItem textDocumentItem) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document xmlParsed = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(textDocumentItem.getText().getBytes(StandardCharsets.UTF_8)));
		for (String camelNodeTag : CAMEL_NODES_TAG) {
			if(hasElementFromCamelNameSpaces(xmlParsed.getElementsByTagName(camelNodeTag))){
				return true;
			}
		}
		return false;
	}

	private Node findElementAtLine(int line, Node node) {
		if(CAMEL_NODES_TAG.contains(node.getNodeName())) {
			return node;
		}
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			String nodeLineStart = (String)childNode.getUserData(XmlLineNumberParser.LINE_NUMBER);
			String nodeLineEnd = (String)childNode.getUserData(XmlLineNumberParser.LINE_NUMBER_END);
			// -1 is due to Camel XMLLineParser which is starting index at 1 although LSP is starting at 0
			if (nodeLineStart != null && nodeLineEnd != null && isBetween(line, Integer.parseInt(nodeLineStart) - 1, Integer.parseInt(nodeLineEnd) - 1)) {
				return findElementAtLine(line, childNode);
			}
		}
		return null;
	}

	private boolean hasElementFromCamelNameSpaces(NodeList childNodes) {
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (NAMESPACEURI_CAMEL_BLUEPRINT.equals(child.getNamespaceURI()) || NAMESPACEURI_CAMEL_SPRING.equals(child.getNamespaceURI())) {
				return true;
			}
		}
		return false;
	}
	
}
