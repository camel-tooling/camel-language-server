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
import java.time.Duration;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.RangeChecker;

public abstract class AbstractDiagnosticTest extends AbstractCamelLanguageServerTest {

	protected static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(10000);
	protected CamelLanguageServer camelLanguageServer;

	protected void checkRange(Range range, int startLine, int startCharacter, int endLine, int endCharacter) {
		new RangeChecker().check(range, startLine, startCharacter, endLine, endCharacter);
	}

	protected void testDiagnostic(String fileUnderTest, int expectedNumberOfError, String extension) throws FileNotFoundException {
		File f = new File("src/test/resources/workspace/diagnostic/" + fileUnderTest + extension);
		camelLanguageServer = initializeLanguageServer(new FileInputStream(f), extension);
		
		DidSaveTextDocumentParams params = new DidSaveTextDocumentParams(new TextDocumentIdentifier(DUMMY_URI+extension));
		camelLanguageServer.getTextDocumentService().didSave(params);
		
		await().timeout(AWAIT_TIMEOUT).untilAsserted(() -> assertThat(lastPublishedDiagnostics).isNotNull());
		await().timeout(AWAIT_TIMEOUT).untilAsserted(() -> assertThat(lastPublishedDiagnostics.getDiagnostics()).hasSize(expectedNumberOfError));
		
		checkHasNonEmptyMessage(lastPublishedDiagnostics.getDiagnostics());
	}

	private void checkHasNonEmptyMessage(List<Diagnostic> diagnostics) {
		for (Diagnostic diagnostic : diagnostics) {
			assertThat(diagnostic.getMessage()).isNotEmpty();
		}
		
	}

}
