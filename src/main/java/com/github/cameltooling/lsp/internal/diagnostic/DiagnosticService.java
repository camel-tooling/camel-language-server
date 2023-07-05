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
package com.github.cameltooling.lsp.internal.diagnostic;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.parser.RouteBuilderParser;
import org.apache.camel.parser.XmlRouteParser;
import org.apache.camel.parser.model.CamelEndpointDetails;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.diagnostic.CamelDiagnosticMessage;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public abstract class DiagnosticService {
	
	public static final String APACHE_CAMEL_VALIDATION = "Apache Camel validation";
	protected static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticService.class);
	public static final String ERROR_CODE_UNKNOWN_PROPERTIES = "camel.diagnostic.unknown.properties";
	public static final String ERROR_CODE_INVALID_ENUM = "camel.diagnostic.invalid.enum";
	
	protected CompletableFuture<CamelCatalog> camelCatalog;

	protected DiagnosticService(CompletableFuture<CamelCatalog> camelCatalog) {
		this.camelCatalog = camelCatalog;
	}

	protected void logExceptionValidatingDocument(String docUri, Exception e) {
		LOGGER.warn("Error while trying to validate the document {}", docUri, e);
	}
	
	protected void computeErrorMessage(StringBuilder sb, String syntaxError) {
		if(syntaxError != null) {
			sb.append(syntaxError).append("\n");
		}
	}

	protected void computeErrorMessage(StringBuilder sb, Map<String, String> mapEntryErrors, CamelDiagnosticMessage<Entry<String, String>> errorMsgComputer) {
		if (mapEntryErrors != null) {
			for (Map.Entry<String, String> invalid : mapEntryErrors.entrySet()) {
				sb.append(errorMsgComputer.getErrorMessage(invalid)).append("\n");
			}
		}
	}
	
	protected void computeErrorMessage(StringBuilder sb, Set<String> setOfErrors, CamelDiagnosticMessage<Set<String>> errorMsgComputer) {
		if(setOfErrors != null && !setOfErrors.isEmpty()) {
			sb.append(errorMsgComputer.getErrorMessage(setOfErrors)).append("\n");
		}
	}

	protected List<CamelEndpointDetails> retrieveEndpoints(String fileUri, String camelText) {
		List<CamelEndpointDetails> endpoints = new ArrayList<>();
		if (fileUri.endsWith(".xml")) {
			try {
				XmlRouteParser.parseXmlRouteEndpoints(new ByteArrayInputStream(camelText.getBytes(StandardCharsets.UTF_8)), "", "/"+fileUri, endpoints);
			} catch (Exception e) {
				logExceptionValidatingDocument(fileUri, e);
			}
		} else if(fileUri.endsWith(".java")) {
			try {
				JavaType<?> parsedJavaFile = Roaster.parse(camelText);
				if (parsedJavaFile instanceof JavaClassSource) {
					JavaClassSource clazz = (JavaClassSource) parsedJavaFile;
					RouteBuilderParser.parseRouteBuilderEndpoints(clazz, "", "/"+fileUri, endpoints);
				}
			} catch(Exception e) {
				logExceptionValidatingDocument(fileUri, e);
			}
		}
		return endpoints;
	}

	protected Range computeRange(String fullCamelText, TextDocumentItem textDocumentItem, CamelEndpointDetails camelEndpointDetails) {
		int endLine = camelEndpointDetails.getLineNumberEnd() != null ? Integer.valueOf(camelEndpointDetails.getLineNumberEnd()) - 1 : findLine(fullCamelText, camelEndpointDetails);
		String lineContainingTheCamelURI = new ParserFileHelperUtil().getLine(textDocumentItem, endLine);
		String endpointUri = camelEndpointDetails.getEndpointUri();
		if(textDocumentItem.getUri().endsWith(".xml")) {
			endpointUri = endpointUri.replace("&", "&amp;");
		}
		int startOfUri = lineContainingTheCamelURI.indexOf(endpointUri);
		int startLinePosition;
		int endLinePosition;
		if (startOfUri != -1) {
			startLinePosition = startOfUri;
			endLinePosition = startOfUri + endpointUri.length();
		} else {
			startLinePosition = 0;
			endLinePosition = lineContainingTheCamelURI.length();
		}
		int startLine = camelEndpointDetails.getLineNumber() != null ? Integer.valueOf(camelEndpointDetails.getLineNumber()) - 1 : findLine(fullCamelText, camelEndpointDetails);
		return new Range(new Position(startLine, startLinePosition), new Position(endLine, endLinePosition));
	}

	/**
	 * Computing by hand for Camel versions earlier than the version which will contain https://issues.apache.org/jira/browse/CAMEL-12639
	 * 
	 * @param fullCamelText The whole file to search in
	 * @param camelEndpointDetails The Camel URI searched for
	 * @return the line number of camelEndpointDetails inside fullCamelText
	 */
	protected int findLine(String fullCamelText, CamelEndpointDetails camelEndpointDetails) {
		int currentSearchedLine = 0;
		String str;
		BufferedReader reader = new BufferedReader(new StringReader(fullCamelText));
		try {
			while ((str = reader.readLine()) != null) {
				if (str.contains(camelEndpointDetails.getEndpointUri())) {
					return currentSearchedLine;
				}
				currentSearchedLine++;
			}
		} catch(IOException e) {
			LOGGER.error("Error while computing range of error", e);
		}
		return 0;
	}
}
