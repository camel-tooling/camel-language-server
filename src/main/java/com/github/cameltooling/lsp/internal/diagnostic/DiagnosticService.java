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
package com.github.cameltooling.lsp.internal.diagnostic;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.parser.RouteBuilderParser;
import org.apache.camel.parser.XmlRouteParser;
import org.apache.camel.parser.model.CamelEndpointDetails;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.CamelEndpointDetailsWrapper;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.parser.ParserXMLFileHelper;
import com.github.cameltooling.model.diagnostic.BooleanErrorMsg;
import com.github.cameltooling.model.diagnostic.CamelDiagnosticEndpointMessage;
import com.github.cameltooling.model.diagnostic.EnumErrorMsg;
import com.github.cameltooling.model.diagnostic.IntegerErrorMsg;
import com.github.cameltooling.model.diagnostic.NumberErrorMsg;
import com.github.cameltooling.model.diagnostic.ReferenceErrorMsg;
import com.github.cameltooling.model.diagnostic.UnknownErrorMsg;

public class DiagnosticService {
	
	private static final String APACHE_CAMEL_VALIDATION = "Apache Camel validation";
	private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticService.class);
	
	private CompletableFuture<CamelCatalog> camelCatalog;
	private CamelLanguageServer camelLanguageServer;

	public DiagnosticService(CompletableFuture<CamelCatalog> camelCatalog, CamelLanguageServer camelLanguageServer) {
		this.camelCatalog = camelCatalog;
		this.camelLanguageServer = camelLanguageServer;
	}

	public void compute(DidSaveTextDocumentParams params) {
		String camelText = retrieveFullText(params);
		Map<CamelEndpointDetailsWrapper, EndpointValidationResult> endpointErrors = computeCamelErrors(camelText, params);
		List<Diagnostic> diagnostics = converToLSPDiagnostics(camelText, endpointErrors, camelLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()));
		PublishDiagnosticsParams diagnosticParam = new PublishDiagnosticsParams(params.getTextDocument().getUri(), diagnostics);
		camelLanguageServer.getClient().publishDiagnostics(diagnosticParam);
	}

	private Map<CamelEndpointDetailsWrapper, EndpointValidationResult> computeCamelErrors(String camelText, DidSaveTextDocumentParams params) {
		List<CamelEndpointDetails> endpoints = retrieveEndpoints(params, camelText);
		return diagnoseEndpoints(params, endpoints);
	}

	private String retrieveFullText(DidSaveTextDocumentParams params) {
		String camelText = params.getText();
		if (camelText == null) {
			camelText = camelLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()).getText();
		}
		return camelText;
	}

	private Map<CamelEndpointDetailsWrapper, EndpointValidationResult> diagnoseEndpoints(DidSaveTextDocumentParams params, List<CamelEndpointDetails> endpoints) {
		Map<CamelEndpointDetailsWrapper, EndpointValidationResult> endpointErrors = new HashMap<>();
		try {
			CamelCatalog camelCatalogResolved = camelCatalog.get();
			for (CamelEndpointDetails camelEndpointDetails : endpoints) {
				EndpointValidationResult validateEndpointProperties = camelCatalogResolved.validateEndpointProperties(camelEndpointDetails.getEndpointUri(), true);
				if (validateEndpointProperties.hasErrors()) {
					endpointErrors.put(new CamelEndpointDetailsWrapper(camelEndpointDetails), validateEndpointProperties);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logExceptionValidatingDocument(params, e);
		} catch (ExecutionException e) {
			logExceptionValidatingDocument(params, e);
		}
		return endpointErrors;
	}

	private List<CamelEndpointDetails> retrieveEndpoints(DidSaveTextDocumentParams params, String camelText) {
		List<CamelEndpointDetails> endpoints = new ArrayList<>();
		String uri = params.getTextDocument().getUri();
		if (uri.endsWith(".xml")) {
			try {
				XmlRouteParser.parseXmlRouteEndpoints(new ByteArrayInputStream(camelText.getBytes(StandardCharsets.UTF_8)), "", "/"+uri, endpoints);
			} catch (Exception e) {
				logExceptionValidatingDocument(params, e);
			}
		} else if(uri.endsWith(".java")) {
			JavaClassSource clazz = (JavaClassSource) Roaster.parse(camelText);
			RouteBuilderParser.parseRouteBuilderEndpoints(clazz, "", "/"+uri, endpoints);
		}
		return endpoints;
	}

	private void logExceptionValidatingDocument(DidSaveTextDocumentParams params, Exception e) {
		LOGGER.warn("Error while trying to validate the document " + params.getTextDocument().getUri(), e);
	}

	private List<Diagnostic> converToLSPDiagnostics(String fullCamelText, Map<CamelEndpointDetailsWrapper, EndpointValidationResult> endpointErrors, TextDocumentItem textDocumentItem) {
		List<Diagnostic> diagnostics = new ArrayList<>();
		for (Map.Entry<CamelEndpointDetailsWrapper, EndpointValidationResult> endpointError : endpointErrors.entrySet()) {
			EndpointValidationResult validationResult = endpointError.getValue();
			CamelEndpointDetails camelEndpointDetails = endpointError.getKey().getCamelEndpointDetails();
			diagnostics.add(new Diagnostic(
					computeRange(fullCamelText, textDocumentItem, camelEndpointDetails),
					computeErrorMessage(validationResult),
					DiagnosticSeverity.Error,
					APACHE_CAMEL_VALIDATION,
					null));
		}
		return diagnostics;
	}

	private Range computeRange(String fullCamelText, TextDocumentItem textDocumentItem, CamelEndpointDetails camelEndpointDetails) {
		int endLine = camelEndpointDetails.getLineNumberEnd() != null ? Integer.valueOf(camelEndpointDetails.getLineNumberEnd()) - 1 : findLine(fullCamelText, camelEndpointDetails);
		int endLineSize = new ParserXMLFileHelper().getLine(textDocumentItem, endLine).length();
		int startLine = camelEndpointDetails.getLineNumber() != null ? Integer.valueOf(camelEndpointDetails.getLineNumber()) - 1 : findLine(fullCamelText, camelEndpointDetails);
		return new Range(new Position(startLine, 0), new Position(endLine, endLineSize));
	}

	/**
	 * Computing by hand for Camel versions earlier than the version which will contain https://issues.apache.org/jira/browse/CAMEL-12639
	 * 
	 * @param fullCamelText
	 * @param camelEndpointDetails
	 * @return
	 */
	private int findLine(String fullCamelText, CamelEndpointDetails camelEndpointDetails) {
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
	
	private String computeErrorMessage(EndpointValidationResult validationResult) {
		StringBuilder sb = new StringBuilder();
		computeErrorMessage(validationResult, sb, validationResult.getInvalidInteger(), new IntegerErrorMsg());
		computeErrorMessage(validationResult, sb, validationResult.getInvalidNumber(), new NumberErrorMsg());
		computeErrorMessage(validationResult, sb, validationResult.getInvalidBoolean(), new BooleanErrorMsg());
		computeErrorMessage(validationResult, sb, validationResult.getInvalidReference(), new ReferenceErrorMsg());
		computeErrorMessage(validationResult, sb, validationResult.getInvalidEnum(), new EnumErrorMsg());
		computeErrorMessage(validationResult, sb, validationResult.getUnknown(), new UnknownErrorMsg());
		return sb.toString();
	}

	private void computeErrorMessage(EndpointValidationResult validationResult, StringBuilder sb, Map<String, String> mapEntryErrors, CamelDiagnosticEndpointMessage<Entry<String, String>> errorMsgComputer) {
		if (mapEntryErrors != null) {
			for (Map.Entry<String, String> invalid : mapEntryErrors.entrySet()) {
				sb.append(errorMsgComputer.getErrorMessage(validationResult, invalid)).append("\n");
			}
		}
	}

	private void computeErrorMessage(EndpointValidationResult validationResult, StringBuilder sb, Set<String> entryErrors, CamelDiagnosticEndpointMessage<String> errorMsgComputer) {
		if (entryErrors != null) {
			for (String invalid : entryErrors) {
				sb.append(errorMsgComputer.getErrorMessage(validationResult, invalid)).append("\n");
			}
		}
	}
}