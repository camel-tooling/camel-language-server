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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import java.time.Duration;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

public class CamelDiagnosticTest extends AbstractCamelLanguageServerTest {

	private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10);
	private CamelLanguageServer camelLanguageServer;

	@Test
	public void testNoValidationError() throws Exception {
		testDiagnostic("camel-with-endpoint", 0, ".xml");
	}
	
	@Test
	public void testValidationError() throws Exception {
		testDiagnostic("camel-with-endpoint-error", 1, ".xml");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		assertThat(range.getStart().getLine()).isEqualTo(8);
		assertThat(range.getStart().getCharacter()).isEqualTo(16);
		assertThat(range.getEnd().getLine()).isEqualTo(8);
		assertThat(range.getEnd().getCharacter()).isEqualTo(39);
	}
	
	@Test
	public void testValidationErrorWithNamespacePrefix() throws Exception {
		testDiagnostic("camel-with-endpoint-error-withNamespacePrefix", 1, ".xml");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		assertThat(range.getStart().getLine()).isEqualTo(8);
		assertThat(range.getStart().getCharacter()).isEqualTo(25);
		assertThat(range.getEnd().getLine()).isEqualTo(8);
		assertThat(range.getEnd().getCharacter()).isEqualTo(48);
	}
	
	@Test
	public void testValidationSeveralErrors() throws Exception {
		testDiagnostic("camel-with-endpoint-2-errors", 2, ".xml");
	}
	
	@Test
	public void testInvalidBoolean() throws Exception {
		testDiagnostic("camel-with-endpoint-boolean-error", 1, ".xml");
	}
	
	@Test
	public void testInvalidInteger() throws Exception {
		testDiagnostic("camel-with-endpoint-integer-error", 1, ".xml");
	}
	
	@Test
	public void testValidationErrorWithSyntaxError() throws Exception {
		testDiagnostic("camel-with-endpoint-error-withampersand", 1, ".xml");
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		assertThat(diagnostic.getMessage()).isNotNull();
		Range range = diagnostic.getRange();
		assertThat(range.getStart().getLine()).isEqualTo(8);
		assertThat(range.getStart().getCharacter()).isEqualTo(16);
		assertThat(range.getEnd().getLine()).isEqualTo(8);
		assertThat(range.getEnd().getCharacter()).isEqualTo(43);
	}
	
	@Test
	public void testNoErrorOnNonCamelFile() throws Exception {
		testDiagnostic("non-camel-file", 0, ".xml");
	}
	
	@Test
	public void testValidationErrorForJavaFile() throws Exception {
		testDiagnostic("camel-with-endpoint-error", 1, ".java");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		assertThat(range.getStart().getLine()).isEqualTo(12);
		assertThat(range.getStart().getCharacter()).isEqualTo(14);
		assertThat(range.getEnd().getLine()).isEqualTo(12);
		assertThat(range.getEnd().getCharacter()).isEqualTo(37);
	}
	
	@Test
	public void testValidationErrorClearedOnClose() throws Exception {
		testDiagnostic("camel-with-endpoint-error", 1, ".xml");
		
		DidCloseTextDocumentParams params = new DidCloseTextDocumentParams(new TextDocumentIdentifier(DUMMY_URI+".xml"));
		camelLanguageServer.getTextDocumentService().didClose(params);
		
		await().timeout(AWAIT_TIMEOUT).untilAsserted(() -> assertThat(lastPublishedDiagnostics.getDiagnostics()).isEmpty());
	}
	
	@Test
	public void testValidationErrorUpdatedOnChange() throws Exception {
		testDiagnostic("camel-with-endpoint-error", 1, ".xml");
		
		camelLanguageServer.getTextDocumentService().getOpenedDocument(DUMMY_URI+".xml").getText();
		DidChangeTextDocumentParams params = new DidChangeTextDocumentParams();
		params.setTextDocument(new VersionedTextDocumentIdentifier(DUMMY_URI+".xml", 2));
		List<TextDocumentContentChangeEvent> contentChanges = new ArrayList<>();
		contentChanges.add(new TextDocumentContentChangeEvent("<from uri=\"timer:timerName?delay=1000\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n"));
		params.setContentChanges(contentChanges);
		camelLanguageServer.getTextDocumentService().didChange(params);
		
		await().timeout(AWAIT_TIMEOUT).untilAsserted(() -> assertThat(lastPublishedDiagnostics.getDiagnostics()).isEmpty());
	}
	
	@Test
	@Disabled("Not yet supported by Camel, see CAMEL-13382")
	public void testNoErrorWithProperty() throws Exception {
		testDiagnostic("camel-with-properties", 0, ".xml");
	}
	
	@Test
	public void testUnknowPropertyOnNonLenientPropertiesComponent() throws Exception {
		testDiagnostic("camel-with-unknownParameter", 1, ".xml");
	}
	
	@Test
	public void testUnknowPropertyOnLenientPropertiesComponent() throws Exception {
		testDiagnostic("camel-with-unknownParameter-forlenientcomponent", 0, ".xml");
	}
	
	private void testDiagnostic(String fileUnderTest, int expectedNumberOfError, String extension) throws FileNotFoundException {
		File f = new File("src/test/resources/workspace/diagnostic/" + fileUnderTest + extension);
		camelLanguageServer = initializeLanguageServer(new FileInputStream(f), extension);
		
		DidSaveTextDocumentParams params = new DidSaveTextDocumentParams(new TextDocumentIdentifier(DUMMY_URI+extension));
		camelLanguageServer.getTextDocumentService().didSave(params);
		
		await().timeout(AWAIT_TIMEOUT).untilAsserted(() -> assertThat(lastPublishedDiagnostics).isNotNull());
		await().timeout(AWAIT_TIMEOUT).untilAsserted(() -> assertThat(lastPublishedDiagnostics.getDiagnostics()).hasSize(expectedNumberOfError));
	}
	
}
