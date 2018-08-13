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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.camel.parser.helper.XmlLineNumberParser;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;

public class ParserXMLFileHelper extends ParserFileHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ParserXMLFileHelper.class);
	
	protected static final List<String> CAMEL_POSSIBLE_TYPES = Arrays.asList("to", "from", "endpoint");
	protected static final List<Character> POSSIBLE_URI_CLOSURE_CHARS = Arrays.asList('\"', '\'');
	
	private static final String ATTRIBUTE_ROUTE = "route";
	private static final String ATTRIBUTE_CAMEL_CONTEXT = "camelContext";
	private static final String NAMESPACEURI_CAMEL_BLUEPRINT = "http://camel.apache.org/schema/blueprint";
	private static final String NAMESPACEURI_CAMEL_SPRING = "http://camel.apache.org/schema/spring";
	private static final List<String> DOCUMENT_SYMBOL_POSSIBLE_TYPES = Arrays.asList(ATTRIBUTE_CAMEL_CONTEXT, ATTRIBUTE_ROUTE);
	private static final String URI_PARAM = "uri=";
	
	public String getCamelComponentUri(String line, int characterPosition) {
		int uriAttribute = line.indexOf(URI_PARAM);
		if(uriAttribute != -1) {
			int firstQuote = uriAttribute + URI_PARAM.length();
			Character closure = line.charAt(firstQuote);
			if (POSSIBLE_URI_CLOSURE_CHARS.contains(closure)) {
				int nextQuote = line.indexOf(closure, firstQuote+1);
				if (isBetween(characterPosition, firstQuote, nextQuote)) {
					return line.substring(firstQuote+1, nextQuote);
				}
			} else {
				LOGGER.warn("Encountered an unsupported URI closure char {}", closure);
			}
		}
		return null;
	}
	
	/**
	 * @param textDocumentItem	the text document item
	 * @param line 	the line number
	 * @return Currently returns the first from Camel Node ignoring the exact position
	 */
	public Node getCorrespondingCamelNodeForCompletion(TextDocumentItem textDocumentItem, int line) {
		try {
			if (hasElementFromCamelNamespace(textDocumentItem)) {
				Document parseXml = XmlLineNumberParser.parseXml(new ByteArrayInputStream(textDocumentItem.getText().getBytes(StandardCharsets.UTF_8)));
				Element documentElement = parseXml.getDocumentElement();
				return findElementAtLine(line, documentElement);
			} else {
				return null;
			}
		} catch (Exception e) {
			LOGGER.warn("Exception while trying to parse the file", e);
			return null;
		}
	}

	private boolean hasElementFromCamelNamespace(TextDocumentItem textDocumentItem) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		Document xmlParsed = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(textDocumentItem.getText().getBytes(StandardCharsets.UTF_8)));
		Set<String> interestingCamelNodeType = new HashSet<>(CAMEL_POSSIBLE_TYPES);
		interestingCamelNodeType.addAll(DOCUMENT_SYMBOL_POSSIBLE_TYPES);
		for (String camelNodeTag : interestingCamelNodeType) {
			if(hasElementFromCamelNameSpaces(xmlParsed.getElementsByTagName(camelNodeTag))){
				return true;
			}
		}
		return false;
	}

	private Node findElementAtLine(int line, Node node) {
		if(CAMEL_POSSIBLE_TYPES.contains(node.getNodeName())) {
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

	public NodeList getRouteNodes(TextDocumentItem textDocumentItem) throws Exception {
		return getNodesOfType(textDocumentItem, ATTRIBUTE_ROUTE);
	}

	public NodeList getCamelContextNodes(TextDocumentItem textDocumentItem) throws Exception {
		return getNodesOfType(textDocumentItem, ATTRIBUTE_CAMEL_CONTEXT);
	}
	
	private NodeList getNodesOfType(TextDocumentItem textDocumentItem, String attributeTypeToFilter) throws Exception {
		if (hasElementFromCamelNamespace(textDocumentItem)) {
			Document parsedXml = XmlLineNumberParser.parseXml(new ByteArrayInputStream(textDocumentItem.getText().getBytes(StandardCharsets.UTF_8)));
			return parsedXml.getElementsByTagName(attributeTypeToFilter);
		}
		return null;
	}

	@Override
	public CamelURIInstance createCamelURIInstance(TextDocumentItem textDocumentItem, Position position, String camelComponentUri) {
		Node correspondingCamelNode = getCorrespondingCamelNodeForCompletion(textDocumentItem, position.getLine());
		return new CamelURIInstance(camelComponentUri, correspondingCamelNode);
	}

	@Override
	public int getPositionInCamelURI(TextDocumentItem textDocumentItem, Position position) {
		return position.getCharacter() - getLine(textDocumentItem, position).indexOf("uri=") - 5;
	}
}
