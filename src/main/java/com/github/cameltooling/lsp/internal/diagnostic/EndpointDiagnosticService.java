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
import org.apache.camel.parser.model.CamelEndpointDetails;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.catalog.diagnostic.BooleanErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.EnumErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.IntegerErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.InvalidDurationErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.NumberErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.ReferenceErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.RequiredErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.UnknownErrorMsg;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public class EndpointDiagnosticService extends DiagnosticService {

	public EndpointDiagnosticService(CompletableFuture<CamelCatalog> camelCatalog) {
		super(camelCatalog, null);
	}
	
	Map<CamelEndpointDetails, EndpointValidationResult> computeCamelEndpointErrors(String camelText, String uri) {
		List<CamelEndpointDetails> endpoints = retrieveEndpoints(uri, camelText);
		return diagnoseEndpoints(uri, endpoints);
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

	public List<Diagnostic> converToLSPDiagnostics(String fullCamelText, Map<CamelEndpointDetails, EndpointValidationResult> endpointErrors, TextDocumentItem textDocumentItem) {
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
						new UnknownErrorMsg().getErrorMessage(unknownParameter),
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

	private String computeErrorMessage(EndpointValidationResult validationResult) {
		StringBuilder sb = new StringBuilder();
		computeErrorMessage(sb, validationResult.getInvalidInteger(), new IntegerErrorMsg());
		computeErrorMessage(sb, validationResult.getInvalidNumber(), new NumberErrorMsg());
		computeErrorMessage(sb, validationResult.getInvalidBoolean(), new BooleanErrorMsg());
		computeErrorMessage(sb, validationResult.getInvalidReference(), new ReferenceErrorMsg());
		computeErrorMessage(sb, validationResult.getInvalidDuration(), new InvalidDurationErrorMsg());
		computeErrorMessage(sb, validationResult.getRequired(), new RequiredErrorMsg());
		computeErrorMessage(sb, validationResult.getSyntaxError());
		return sb.toString();
	}

}
