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
package com.github.cameltooling.lsp.internal.codeactions;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.RangeChecker;

class InvalidEnumQuickfixTest extends AbstractQuickFixTest {
	
	@Test
	void testReturnCodeActionForQuickfix() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("camel-with-invalid-enum.xml");
	
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		CompletableFuture<List<Either<Command, CodeAction>>> codeActions = retrieveCodeActions(textDocumentIdentifier, diagnostic);
		
		checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActions);
	}
	
	private void checkRetrievedCodeAction(TextDocumentIdentifier textDocumentIdentifier, Diagnostic diagnostic, CompletableFuture<List<Either<Command, CodeAction>>> codeActions)
			throws InterruptedException, ExecutionException {
		TextEdit textEdit = retrieveTextEdit(textDocumentIdentifier, diagnostic, codeActions);
		Range range = textEdit.getRange();
		new RangeChecker().check(range, 9, 49, 9, 54);
		assertThat(textEdit.getNewText()).isEqualTo("InOnly");
	}

}
