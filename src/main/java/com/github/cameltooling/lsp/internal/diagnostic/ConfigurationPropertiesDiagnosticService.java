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
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.ConfigurationPropertiesValidationResult;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.github.cameltooling.lsp.internal.catalog.diagnostic.BooleanErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.EnumErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.IntegerErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.NumberErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.ReferenceErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.UnknownErrorMsg;

public class ConfigurationPropertiesDiagnosticService extends DiagnosticService {

	public ConfigurationPropertiesDiagnosticService(CompletableFuture<CamelCatalog> camelCatalog) {
		super(camelCatalog, null);
	}
	
	public Map<String, ConfigurationPropertiesValidationResult> computeCamelConfigurationPropertiesErrors(String camelText, String uri) {
		Map<String, ConfigurationPropertiesValidationResult> errors = new HashMap<>();
		if (uri.endsWith(".properties")) {
			BufferedReader bufReader = new BufferedReader(new StringReader(camelText));
			String line=null;
			int lineNumber = 0;
			try {
				while((line=bufReader.readLine()) != null){
					ConfigurationPropertiesValidationResult validationResult = camelCatalog.get().validateConfigurationProperty(line);
					if(validationResult.hasErrors() && validationResult.getIncapable() == null) {
						validationResult.setLineNumber(lineNumber);
						errors.put(line, validationResult);
					}
					lineNumber++;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logExceptionValidatingDocument(uri, e);
			} catch (ExecutionException | IOException e) {
				logExceptionValidatingDocument(uri, e);
			}
		}
		return errors;
	}
	
	public Collection<Diagnostic> converToLSPDiagnostics(Map<String, ConfigurationPropertiesValidationResult> configurationPropertiesErrors) {
		List<Diagnostic> lspDiagnostics = new ArrayList<>();
		for (Map.Entry<String, ConfigurationPropertiesValidationResult> errorEntry : configurationPropertiesErrors.entrySet()) {
			ConfigurationPropertiesValidationResult validationResult = errorEntry.getValue();
			String lineContentInError = errorEntry.getKey();
			List<Diagnostic> unknownParameterDiagnostics = computeUnknowParameters(validationResult, lineContentInError);
			lspDiagnostics.addAll(unknownParameterDiagnostics);
			List<Diagnostic> invalidEnumDiagnostics = computeInvalidEnumsDiagnostic(validationResult, lineContentInError);
			lspDiagnostics.addAll(invalidEnumDiagnostics);
			if (invalidEnumDiagnostics.size() + unknownParameterDiagnostics.size() < validationResult.getNumberOfErrors()) {
				lspDiagnostics.add(new Diagnostic(
					computeRange(validationResult, lineContentInError, lineContentInError),
					computeErrorMessage(validationResult),
					DiagnosticSeverity.Error,
					APACHE_CAMEL_VALIDATION,
					null));
			}
		}
		return lspDiagnostics;
	}

	private List<Diagnostic> computeInvalidEnumsDiagnostic(ConfigurationPropertiesValidationResult validationResult, String lineContentInError) {
		List<Diagnostic> lspDiagnostics = new ArrayList<>();
		Map<String, String> invalidEnums = validationResult.getInvalidEnum();
		if (invalidEnums != null) {
			for (Entry<String, String> invalidEnum : invalidEnums.entrySet()) {
				lspDiagnostics.add(new Diagnostic(
						computeRange(validationResult, lineContentInError, invalidEnum.getKey()),
						new EnumErrorMsg().getErrorMessage(validationResult, invalidEnum),
						DiagnosticSeverity.Error,
						APACHE_CAMEL_VALIDATION,
						ERROR_CODE_INVALID_ENUM));
			}
		}
		return lspDiagnostics;
	}

	private Range computeRange(ConfigurationPropertiesValidationResult validationResult, String lineContentInError, String value) {
		int startCharacter;
		int endCharacter;
		int indexOfEnum = lineContentInError.indexOf(value);
		if(indexOfEnum != -1) {
			startCharacter = indexOfEnum;
			endCharacter = indexOfEnum + value.length();
		} else {
			startCharacter = 0;
			endCharacter = lineContentInError.length();
		}
		int lineNumber = validationResult.getLineNumber();
		return new Range(new Position(lineNumber, startCharacter), new Position(lineNumber, endCharacter));
	}

	private List<Diagnostic> computeUnknowParameters(ConfigurationPropertiesValidationResult validationResult, String lineContentInError) {
		List<Diagnostic> lspDiagnostics = new ArrayList<>();
		Set<String> unknownParameters = validationResult.getUnknown();
		if (unknownParameters != null) {
			for (String unknownParameter : unknownParameters) {
				int lastIndexOf = unknownParameter.lastIndexOf('.');
				String realValueOfUnknowparameter;
				if(lastIndexOf != -1) {
					realValueOfUnknowparameter = unknownParameter.substring(lastIndexOf + 1);
				} else {
					realValueOfUnknowparameter = unknownParameter;
				}
				lspDiagnostics.add(new Diagnostic(
						computeRange(validationResult, lineContentInError, realValueOfUnknowparameter),
						new UnknownErrorMsg().getErrorMessage(unknownParameter),
						DiagnosticSeverity.Error,
						APACHE_CAMEL_VALIDATION,
						ERROR_CODE_UNKNOWN_PROPERTIES));
			}
		}
		return lspDiagnostics;
	}

	private String computeErrorMessage(ConfigurationPropertiesValidationResult validationResult) {
		StringBuilder sb = new StringBuilder();
		computeErrorMessage(sb, validationResult.getInvalidInteger(), new IntegerErrorMsg());
		computeErrorMessage(sb, validationResult.getInvalidNumber(), new NumberErrorMsg());
		computeErrorMessage(sb, validationResult.getInvalidBoolean(), new BooleanErrorMsg());
		computeErrorMessage(sb, validationResult.getInvalidReference(), new ReferenceErrorMsg());
		computeErrorMessage(sb, validationResult.getSyntaxError());
		return sb.toString();
	}

}
