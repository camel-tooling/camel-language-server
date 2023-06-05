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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

class CamelDiagnosticTest extends AbstractDiagnosticTest {

	@Test
	void testNoValidationError() throws Exception {
		testDiagnostic("camel-with-endpoint", 0, ".xml");
	}
	
	@Test
	void testValidationError() throws Exception {
		testDiagnostic("camel-with-endpoint-error", 1, ".xml");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range, 8, 16, 8, 39);
	}

	@Test
	void testValidationErrorWithNamespacePrefix() throws Exception {
		testDiagnostic("camel-with-endpoint-error-withNamespacePrefix", 1, ".xml");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range, 8, 25, 8, 48);
	}
	
	@Test
	void testValidationSeveralErrors() throws Exception {
		testDiagnostic("camel-with-endpoint-2-errors", 2, ".xml");
	}
	
	@Test
	void testValidationForNonFirstParameters() throws Exception {
		testDiagnostic("camel-with-endpoint-error-on-second-parameter", 1, ".xml");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range, 8, 16, 8, 61);
	}
	
	@Test
	void testInvalidBoolean() throws Exception {
		testDiagnostic("camel-with-endpoint-boolean-error", 1, ".xml");
	}
	
	@Test
	void testInvalidInteger() throws Exception {
		testDiagnostic("camel-with-endpoint-integer-error", 1, ".xml");
	}
	
	@Test
	void testInvalidEnum() throws Exception {
		testDiagnostic("camel-with-invalid-enum", 1, ".xml");
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		assertThat(diagnostic.getMessage()).isNotNull();
		Range range = diagnostic.getRange();
		checkRange(range, 9, 49, 9, 54);
	}
	
	@Test
	void testInvalidEnumWithSameStringOnSameLine() throws Exception {
		testDiagnostic("camel-with-invalid-enum-with-same-string-in-camel-uri", 1, ".xml");
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		assertThat(diagnostic.getMessage()).isNotNull();
		Range range = diagnostic.getRange();
		checkRange(range, 9, 56, 9, 72);
	}
	
	@Test
	void testMissingMethodNameForApiBasedComponent() throws Exception {
		testDiagnostic("camel-with-missing-methodname", 2, ".xml");
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		assertThat(diagnostic.getMessage()).isNotNull();
		Range range = diagnostic.getRange();
		checkRange(range, 9, 17, 9, 32);
		
		Diagnostic diagnostic2 = lastPublishedDiagnostics.getDiagnostics().get(1);
		assertThat(diagnostic2.getMessage()).isNotNull();
		Range range2 = diagnostic2.getRange();
		checkRange(range2, 9, 17, 9, 32);
	}
	
	@Test
	void testValidationErrorWithSyntaxError() throws Exception {
		testDiagnostic("camel-with-endpoint-error-withampersand", 1, ".xml");
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		assertThat(diagnostic.getMessage()).isNotNull();
		Range range = diagnostic.getRange();
		checkRange(range, 8, 16, 8, 47);
	}
	
	@Test
	void testNoErrorOnNonCamelFile() throws Exception {
		testDiagnostic("non-camel-file", 0, ".xml");
	}
	
	@Test
	void testNoErrorWithPropertyForWholeURI() throws Exception {
		testDiagnostic("camel-with-properties", 0, ".java");
	}
	
	@Test
	void testValidationErrorForJavaFile() throws Exception {
		testDiagnostic("camel-with-endpoint-error", 1, ".java");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range, 12, 14, 12, 37);
	}
	
	@Test
	void testNoExceptionOnInvalidJavaFile() throws Exception {
		testDiagnostic("AnInvalid", 0, ".java");
	}
	
	@Test
	void testValidationErrorClearedOnClose() throws Exception {
		testDiagnostic("camel-with-endpoint-error", 1, ".xml");
		
		DidCloseTextDocumentParams params = new DidCloseTextDocumentParams(new TextDocumentIdentifier(DUMMY_URI+".xml"));
		camelLanguageServer.getTextDocumentService().didClose(params);
		
		await().timeout(AWAIT_TIMEOUT.multipliedBy(2)).untilAsserted(() -> assertThat(lastPublishedDiagnostics.getDiagnostics()).isEmpty());
	}
	
	@Test
	void testValidationErrorUpdatedOnChange() throws Exception {
		testDiagnostic("camel-with-endpoint-error", 1, ".xml");
		
		camelLanguageServer.getTextDocumentService().getOpenedDocument(DUMMY_URI+".xml").getText();
		DidChangeTextDocumentParams params = new DidChangeTextDocumentParams();
		params.setTextDocument(new VersionedTextDocumentIdentifier(DUMMY_URI+".xml", 2));
		List<TextDocumentContentChangeEvent> contentChanges = new ArrayList<>();
		contentChanges.add(new TextDocumentContentChangeEvent(RouteTextBuilder.createXMLBlueprintRoute("timer:timerName?delay=1000")));
		params.setContentChanges(contentChanges);
		camelLanguageServer.getTextDocumentService().didChange(params);
		
		await().timeout(AWAIT_TIMEOUT).untilAsserted(() -> assertThat(lastPublishedDiagnostics.getDiagnostics()).isEmpty());
	}
	
	@Test
	@Disabled("Not yet supported by Camel, see CAMEL-13382")
	void testNoErrorWithProperty() throws Exception {
		testDiagnostic("camel-with-properties", 0, ".xml");
	}
	
	@Test
	void testUnknowPropertyOnNonLenientPropertiesComponent() throws Exception {
		testDiagnostic("camel-with-unknownParameter", 1, ".xml");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range, 9, 33, 9, 37);
	}

	@Test
	void testSeveralUnknowPropertyOnNonLenientPropertiesComponent() throws Exception {
		testDiagnostic("camel-with-2-unknownParameters", 2, ".xml");
		Range range1 = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range1, 9, 33, 9, 46);
		Range range2 = lastPublishedDiagnostics.getDiagnostics().get(1).getRange();
		checkRange(range2, 9, 56, 9, 69);
	}
	
	@Test
	void testSeveralErrorsWithPreciseSpecificrange() throws Exception {
		testDiagnostic("camel-with-several-errors-with-precise-specific-range", 3, ".xml");
		Range range1 = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range1, 9, 33, 9, 46);
		Range range2 = lastPublishedDiagnostics.getDiagnostics().get(1).getRange();
		checkRange(range2, 9, 56, 9, 69);
		Range range3 = lastPublishedDiagnostics.getDiagnostics().get(2).getRange();
		checkRange(range3, 9, 95, 9, 111);
	}

	@Test
	void testSeveralUnknowPropertyAndAnotherError() throws Exception {
		testDiagnostic("camel-with-unknownParameterAndAnotherError", 2, ".xml");
	}
	
	@Test
	void testUnknowPropertyOnLenientPropertiesComponent() throws Exception {
		testDiagnostic("camel-with-unknownParameter-forlenientcomponent", 0, ".xml");
	}
	
	@Test
	void testJavaInterface() throws Exception {
		testDiagnostic("AnInterface", 0, ".java");
	}
}
