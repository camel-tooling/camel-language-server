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
import java.util.Collections;
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
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;
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
	public static final String ERROR_CODE_UNKNOWN_PROPERTIES = "camel.diagnostic.unknown.properties";
	public static final String ERROR_CODE_INVALID_ENUM = "camel.diagnostic.invalid.enum";
	
	private CompletableFuture<CamelCatalog> camelCatalog;
	private CamelLanguageServer camelLanguageServer;

	public DiagnosticService(CompletableFuture<CamelCatalog> camelCatalog, CamelLanguageServer camelLanguageServer) {
		this.camelCatalog = camelCatalog;
		this.camelLanguageServer = camelLanguageServer;
	}

	public void compute(DidSaveTextDocumentParams params) {
		String camelText = retrieveFullText(params);
		computeDiagnostics(camelText, params.getTextDocument().getUri());
	}
	
	public void compute(DidChangeTextDocumentParams params) {
		String camelText = params.getContentChanges().get(0).getText();
		computeDiagnostics(camelText, params.getTextDocument().getUri());
	}
	
	public void compute(DidOpenTextDocumentParams params) {
		String camelText = params.getTextDocument().getText();
		computeDiagnostics(camelText, params.getTextDocument().getUri());
	}
	
	private void computeDiagnostics(String camelText, String uri) {
		CompletableFuture.supplyAsync(() -> new DiagnosticRunner(this, camelLanguageServer, camelText, uri));
	}
	
	Map<CamelEndpointDetails, EndpointValidationResult> computeCamelEndpointErrors(String camelText, String uri) {
		List<CamelEndpointDetails> endpoints = retrieveEndpoints(uri, camelText);
		return diagnoseEndpoints(uri, endpoints);
	}

	private String retrieveFullText(DidSaveTextDocumentParams params) {
		String camelText = params.getText();
		if (camelText == null) {
			camelText = camelLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()).getText();
		}
		return camelText;
	}

	private Map<CamelEndpointDetails, EndpointValidationResult> diagnoseEndpoints(String uri, List<CamelEndpointDetails> endpoints) {
		Map<CamelEndpointDetails, EndpointValidationResult> endpointErrors = new HashMap<>();
		try {
			CamelCatalog camelCatalogResolved = camelCatalog.get();
			for (CamelEndpointDetails camelEndpointDetails : endpoints) {
				EndpointValidationResult validateEndpointProperties = camelCatalogResolved.validateEndpointProperties(camelEndpointDetails.getEndpointUri(), false);
				if (validateEndpointProperties.hasErrors() && wasCapableToValidate(validateEndpointProperties)) {
					endpointErrors.put(camelEndpointDetails, validateEndpointProperties);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logExceptionValidatingDocument(uri, e);
		} catch (ExecutionException e) {
			logExceptionValidatingDocument(uri, e);
		}
		return endpointErrors;
	}

	private boolean wasCapableToValidate(EndpointValidationResult validateEndpointProperties) {
		return validateEndpointProperties.getIncapable() == null;
	}

	private List<CamelEndpointDetails> retrieveEndpoints(String uri, String camelText) {
		List<CamelEndpointDetails> endpoints = new ArrayList<>();
		if (uri.endsWith(".xml")) {
			try {
				XmlRouteParser.parseXmlRouteEndpoints(new ByteArrayInputStream(camelText.getBytes(StandardCharsets.UTF_8)), "", "/"+uri, endpoints);
			} catch (Exception e) {
				logExceptionValidatingDocument(uri, e);
			}
		} else if(uri.endsWith(".java")) {
			JavaClassSource clazz = (JavaClassSource) Roaster.parse(camelText);
			RouteBuilderParser.parseRouteBuilderEndpoints(clazz, "", "/"+uri, endpoints);
		}
		return endpoints;
	}

	private void logExceptionValidatingDocument(String docUri, Exception e) {
		LOGGER.warn("Error while trying to validate the document {0}", docUri, e);
	}

	List<Diagnostic> converToLSPDiagnostics(String fullCamelText, Map<CamelEndpointDetails, EndpointValidationResult> endpointErrors, TextDocumentItem textDocumentItem) {
		List<Diagnostic> lspDiagnostics = new ArrayList<>();
		for (Map.Entry<CamelEndpointDetails, EndpointValidationResult> endpointError : endpointErrors.entrySet()) {
			EndpointValidationResult validationResult = endpointError.getValue();
			CamelEndpointDetails camelEndpointDetails = endpointError.getKey();
			List<Diagnostic> unknownParameterDiagnostics = computeUnknowParameters(fullCamelText, textDocumentItem, validationResult, camelEndpointDetails);
			lspDiagnostics.addAll(unknownParameterDiagnostics);
			List<Diagnostic> invalidEnumDiagnostics = computeInvalidEnumsDiagnostic(fullCamelText, textDocumentItem, validationResult, camelEndpointDetails);
			lspDiagnostics.addAll(invalidEnumDiagnostics);
			if (invalidEnumDiagnostics.size() + unknownParameterDiagnostics.size() < validationResult.getNumberOfErrors()) {
				lspDiagnostics.add(new Diagnostic(
						computeRange(fullCamelText, textDocumentItem, camelEndpointDetails),
						computeErrorMessage(validationResult),
						DiagnosticSeverity.Error,
						APACHE_CAMEL_VALIDATION,
						null));
			}
		}
		return lspDiagnostics;
	}

	private List<Diagnostic> computeInvalidEnumsDiagnostic(String fullCamelText, TextDocumentItem textDocumentItem, EndpointValidationResult validationResult, CamelEndpointDetails camelEndpointDetails) {
		List<Diagnostic> lspDiagnostics = new ArrayList<>();
		Map<String, String> invalidEnums = validationResult.getInvalidEnum();
		if (invalidEnums != null) {
			for (Entry<String, String> invalidEnum : invalidEnums.entrySet()) {
				lspDiagnostics.add(new Diagnostic(
						computeRange(fullCamelText, textDocumentItem, camelEndpointDetails, invalidEnum),
						new EnumErrorMsg().getErrorMessage(validationResult, invalidEnum),
						DiagnosticSeverity.Error,
						APACHE_CAMEL_VALIDATION,
						ERROR_CODE_INVALID_ENUM));
			}
		}
		return lspDiagnostics;
	}

	private List<Diagnostic> computeUnknowParameters(String fullCamelText, TextDocumentItem textDocumentItem, EndpointValidationResult validationResult, CamelEndpointDetails camelEndpointDetails) {
		List<Diagnostic> lspDiagnostics = new ArrayList<>();
		Set<String> unknownParameters = validationResult.getUnknown();
		if (unknownParameters != null) {
			for (String unknownParameter : unknownParameters) {
				lspDiagnostics.add(new Diagnostic(
						computeRange(fullCamelText, textDocumentItem, camelEndpointDetails, unknownParameter),
						new UnknownErrorMsg().getErrorMessage(validationResult, unknownParameter),
						DiagnosticSeverity.Error,
						APACHE_CAMEL_VALIDATION,
						ERROR_CODE_UNKNOWN_PROPERTIES));
			}
		}
		return lspDiagnostics;
	}
	
	private Range computeRange(String fullCamelText, TextDocumentItem textDocumentItem, CamelEndpointDetails camelEndpointDetails, Entry<String, String> invalidEnum) {
		int endLine = camelEndpointDetails.getLineNumberEnd() != null ? Integer.valueOf(camelEndpointDetails.getLineNumberEnd()) - 1 : findLine(fullCamelText, camelEndpointDetails);
		int startLine = camelEndpointDetails.getLineNumber() != null ? Integer.valueOf(camelEndpointDetails.getLineNumber()) - 1 : findLine(fullCamelText, camelEndpointDetails);
		if(startLine == endLine) {
			String lineContainingTheCamelURI = new ParserFileHelperUtil().getLine(textDocumentItem, endLine);
			int startCharacterOfProperty = lineContainingTheCamelURI.indexOf(invalidEnum.getKey());
			if (startCharacterOfProperty != -1) {
				int startCharacter = lineContainingTheCamelURI.indexOf(invalidEnum.getValue(), startCharacterOfProperty);
				if (startCharacter != -1) {
					int endCharacter = startCharacter + invalidEnum.getValue().length();
					return new Range(new Position(startLine, startCharacter), new Position(endLine, endCharacter));
				}
			}
		}
		return computeRange(fullCamelText, textDocumentItem, camelEndpointDetails);
	}

	private Range computeRange(String fullCamelText, TextDocumentItem textDocumentItem, CamelEndpointDetails camelEndpointDetails, String unknownParameter) {
		int endLine = camelEndpointDetails.getLineNumberEnd() != null ? Integer.valueOf(camelEndpointDetails.getLineNumberEnd()) - 1 : findLine(fullCamelText, camelEndpointDetails);
		int startLine = camelEndpointDetails.getLineNumber() != null ? Integer.valueOf(camelEndpointDetails.getLineNumber()) - 1 : findLine(fullCamelText, camelEndpointDetails);
		if(startLine == endLine) {
			String lineContainingTheCamelURI = new ParserFileHelperUtil().getLine(textDocumentItem, endLine);
			int startCharacter = lineContainingTheCamelURI.indexOf(unknownParameter);
			if (startCharacter != -1) {
				int endCharacter = startCharacter + unknownParameter.length();
				return new Range(new Position(startLine, startCharacter), new Position(endLine, endCharacter));
			}
		}
		return computeRange(fullCamelText, textDocumentItem, camelEndpointDetails);
	}

	private Range computeRange(String fullCamelText, TextDocumentItem textDocumentItem, CamelEndpointDetails camelEndpointDetails) {
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
		computeErrorMessage(sb, validationResult.getSyntaxError());
		return sb.toString();
	}

	private void computeErrorMessage(StringBuilder sb, String syntaxError) {
		if(syntaxError != null) {
			sb.append(syntaxError).append("\n");
		}
	}

	private void computeErrorMessage(EndpointValidationResult validationResult, StringBuilder sb, Map<String, String> mapEntryErrors, CamelDiagnosticEndpointMessage<Entry<String, String>> errorMsgComputer) {
		if (mapEntryErrors != null) {
			for (Map.Entry<String, String> invalid : mapEntryErrors.entrySet()) {
				sb.append(errorMsgComputer.getErrorMessage(validationResult, invalid)).append("\n");
			}
		}
	}

	public void clear(String uri) {
		camelLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, Collections.emptyList()));
	}

}