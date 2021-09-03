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
package com.github.cameltooling.lsp.internal.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.camel.parser.helper.CamelXmlHelper;
import org.apache.camel.parser.helper.XmlLineNumberParser;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
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
	private static final List<String> CAMEL_NAMESPACE_URIS = Arrays.asList(NAMESPACEURI_CAMEL_BLUEPRINT, NAMESPACEURI_CAMEL_SPRING);
	private static final List<String> DOCUMENT_SYMBOL_POSSIBLE_TYPES = Arrays.asList(ATTRIBUTE_CAMEL_CONTEXT, ATTRIBUTE_ROUTE);
	private static final String URI_PARAM = "uri=";
	
	private String prefixCamelNamespace = null;

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
				Document parseXml = getDocumentWithLineInformation(textDocumentItem);
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

	public boolean hasElementFromCamelNamespace(TextDocumentItem textDocumentItem) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
		dbf.setNamespaceAware(true);
		Document xmlParsed = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(textDocumentItem.getText().getBytes(StandardCharsets.UTF_8)));
		Set<String> interestingCamelNodeType = new HashSet<>(CAMEL_POSSIBLE_TYPES);
		interestingCamelNodeType.addAll(DOCUMENT_SYMBOL_POSSIBLE_TYPES);
		for (String camelNodeTag : interestingCamelNodeType) {
			for (String camelNamespace : CAMEL_NAMESPACE_URIS) {
				if (hasElementFromCamelNameSpaces(xmlParsed.getElementsByTagNameNS(camelNamespace, camelNodeTag))) {
					return true;
				}
			}
		}
		return false;
	}

	private Node findElementAtLine(int line, Node node) {
		if (CAMEL_POSSIBLE_TYPES.contains(prefixCamelNamespace != null ? node.getNodeName().substring(prefixCamelNamespace.length() + 1) : node.getNodeName())) {
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
				prefixCamelNamespace = child.getPrefix();
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
			if (prefixCamelNamespace != null) {
				return parsedXml.getElementsByTagName(prefixCamelNamespace+":"+attributeTypeToFilter);
			} else {
				return parsedXml.getElementsByTagName(attributeTypeToFilter);
			}
		}
		return null;
	}

	private Document getDocumentWithLineInformation(TextDocumentItem textDocumentItem) throws Exception {
		return XmlLineNumberParser.parseXml(new ByteArrayInputStream(textDocumentItem.getText().getBytes(StandardCharsets.UTF_8)));
	}

	@Override
	public CamelURIInstance createCamelURIInstance(TextDocumentItem textDocumentItem, Position position, String camelComponentUri) {
		Node correspondingCamelNode = getCorrespondingCamelNodeForCompletion(textDocumentItem, position.getLine());
		CamelURIInstance uriInstance = new CamelURIInstance(camelComponentUri, correspondingCamelNode, textDocumentItem);
		int start = getStartCharacterInDocumentOnLinePosition(textDocumentItem, position);
		uriInstance.setStartPositionInDocument(new Position(position.getLine(), start));
		uriInstance.setEndPositionInDocument(new Position(position.getLine(), start+camelComponentUri.length()));
		return uriInstance;
	}

	private int getStartCharacterInDocumentOnLinePosition(TextDocumentItem textDocumentItem, Position position) {
		return parserFileHelperUtil.getLine(textDocumentItem, position.getLine()).indexOf(URI_PARAM) + 1 + URI_PARAM.length();
	}

	@Override
	public int getPositionInCamelURI(TextDocumentItem textDocumentItem, Position position) {
		return position.getCharacter() - parserFileHelperUtil.getLine(textDocumentItem, position).indexOf(URI_PARAM) - 5;
	}

	public List<Node> getAllEndpoints(TextDocumentItem textDocumentItem) throws Exception {
		if (hasElementFromCamelNamespace(textDocumentItem)) {
			return CamelXmlHelper.findAllEndpoints(getDocumentWithLineInformation(textDocumentItem));
		}
		return Collections.emptyList();
	}

	public Location retrieveLocation(Node node, TextDocumentItem textDocumentItem) {
		Position startPosition = new Position(retrieveIntUserData(node, XmlLineNumberParser.LINE_NUMBER), retrieveIntUserData(node, XmlLineNumberParser.COLUMN_NUMBER));
		Position endPosition = new Position(retrieveIntUserData(node, XmlLineNumberParser.LINE_NUMBER_END), retrieveIntUserData(node, XmlLineNumberParser.COLUMN_NUMBER_END));
		Range range = new Range(startPosition, endPosition);
		return new Location(textDocumentItem.getUri(), range);
	}
	
	private int retrieveIntUserData(Node node, String userData) {
		return Integer.parseInt((String)node.getUserData(userData)) -1;
	}
}
