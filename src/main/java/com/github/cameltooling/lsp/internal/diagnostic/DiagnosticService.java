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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.parser.XmlRouteParser;
import org.apache.camel.parser.model.CamelEndpointDetails;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
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
		Map<CamelEndpointDetailsWrapper, EndpointValidationResult> endpointErrors = computeCamelErrors(params);
		List<Diagnostic> diagnostics = converToLSPDiagnostics(endpointErrors, camelLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()));
		PublishDiagnosticsParams diagnosticParam = new PublishDiagnosticsParams(params.getTextDocument().getUri(), diagnostics);
		camelLanguageServer.getClient().publishDiagnostics(diagnosticParam);
	}

	private Map<CamelEndpointDetailsWrapper, EndpointValidationResult> computeCamelErrors(DidSaveTextDocumentParams params) {
		Map<CamelEndpointDetailsWrapper, EndpointValidationResult> endpointErrors = new HashMap<>();

		if (params.getTextDocument().getUri().endsWith(".xml")) {
			List<CamelEndpointDetails> endpoints = new ArrayList<>();
			try {
				String camelText = params.getText();
				if (camelText == null) {
					camelText = camelLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()).getText();
				}
				XmlRouteParser.parseXmlRouteEndpoints(new ByteArrayInputStream(camelText.getBytes(StandardCharsets.UTF_8)), "", "/"+params.getTextDocument().getUri(), endpoints);
				for (CamelEndpointDetails camelEndpointDetails : endpoints) {
					EndpointValidationResult validateEndpointProperties = camelCatalog.get().validateEndpointProperties(camelEndpointDetails.getEndpointUri(), true);
					if (validateEndpointProperties.hasErrors()) {
						endpointErrors.put(new CamelEndpointDetailsWrapper(camelEndpointDetails), validateEndpointProperties);
					}
				}
			} catch (Exception e) {
				LOGGER.warn("Error while trying to validate the document " + params.getTextDocument().getUri(), e);
			}
		}
		return endpointErrors;
	}

	private List<Diagnostic> converToLSPDiagnostics(Map<CamelEndpointDetailsWrapper, EndpointValidationResult> endpointErrors, TextDocumentItem textDocumentItem) {
		List<Diagnostic> diagnostics = new ArrayList<>();
		for (Map.Entry<CamelEndpointDetailsWrapper, EndpointValidationResult> endpointError : endpointErrors.entrySet()) {
			EndpointValidationResult validationResult = endpointError.getValue();
			CamelEndpointDetails camelEndpointDetails = endpointError.getKey().getCamelEndpointDetails();
			diagnostics.add(new Diagnostic(
					computeRange(textDocumentItem, camelEndpointDetails),
					computeErrorMessage(validationResult),
					DiagnosticSeverity.Error,
					APACHE_CAMEL_VALIDATION,
					null));
		}
		return diagnostics;
	}

	private Range computeRange(TextDocumentItem textDocumentItem, CamelEndpointDetails camelEndpointDetails) {
		int endLine = Integer.valueOf(camelEndpointDetails.getLineNumberEnd()) - 1;
		int endLineSize = new ParserXMLFileHelper().getLine(textDocumentItem, endLine).length();
		return new Range(new Position(Integer.valueOf(camelEndpointDetails.getLineNumber()) - 1, 0), new Position(endLine, endLineSize));
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