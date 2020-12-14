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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.RangeChecker;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;

class UnknownPropertyQuickfixTest extends AbstractQuickFixTest {

	@Test
	void testReturnCodeActionForQuickfix() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("camel-with-unknownParameter.xml");
	
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = retrieveCodeActions(textDocumentIdentifier, diagnostic);
		
		checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActions);
	}
	
	@Test
	void testReturnCodeActionForQuickfixWhenNoCodeActionKindSpecified() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("camel-with-unknownParameter.xml");
	
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = retrieveCodeActions(textDocumentIdentifier, diagnostic);
		
		checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActions);
	}
	
	@Test
	void testReturnNoCodeActionForOtherThanQuickfix() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("camel-with-unknownParameter.xml");
		
		List<String> codeActionKinds = Stream.of(CodeActionKind.Refactor, CodeActionKind.RefactorExtract, CodeActionKind.RefactorInline, CodeActionKind.RefactorRewrite, CodeActionKind.Source, CodeActionKind.SourceOrganizeImports)
			      .collect(Collectors.toList());
		
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		CodeActionContext context = new CodeActionContext(lastPublishedDiagnostics.getDiagnostics(), codeActionKinds);
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = camelLanguageServer.getTextDocumentService().codeAction(new CodeActionParams(textDocumentIdentifier, diagnostic.getRange(), context));
		
		assertThat(codeActions.get()).isEmpty();
	}
	
	@Test
	void testReturnCodeActionForQuickfixEvenWithInvalidRangeDiagnostic() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("camel-with-unknownParameter.xml");
		
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
	
		List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
		Diagnostic diagnosticWithInvalidRange = new Diagnostic(new Range(new Position(9,100), new Position(9,101)), "a different diagnostic coming with an invalid range.");
		diagnosticWithInvalidRange.setCode(DiagnosticService.ERROR_CODE_UNKNOWN_PROPERTIES);
		diagnostics.add(diagnosticWithInvalidRange);
		diagnostics.addAll(lastPublishedDiagnostics.getDiagnostics());
		
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = retrieveCodeActions(textDocumentIdentifier, diagnostic);
		
		checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActions);
	}
	
	@Test
	void testNoErrorWithDiagnosticWithoutCode() throws FileNotFoundException, InterruptedException, ExecutionException {
		TextDocumentIdentifier textDocumentIdentifier = initAnLaunchDiagnostic("camel-with-unknownParameter.xml");
		
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
	
		List<Diagnostic> diagnostics = new ArrayList<Diagnostic>();
		Diagnostic diagnosticWithoutCode = new Diagnostic(new Range(new Position(9,33), new Position(9,37)), "a different diagnostic coming without code.");
		diagnostics.add(diagnosticWithoutCode);
		diagnostics.addAll(lastPublishedDiagnostics.getDiagnostics());
		
		CompletableFuture<List<Either<Command,CodeAction>>> codeActions = retrieveCodeActions(textDocumentIdentifier, diagnostic);
		
		checkRetrievedCodeAction(textDocumentIdentifier, diagnostic, codeActions);
	}
	
	private void checkRetrievedCodeAction(TextDocumentIdentifier textDocumentIdentifier, Diagnostic diagnostic, CompletableFuture<List<Either<Command, CodeAction>>> codeActions)
			throws InterruptedException, ExecutionException {
		TextEdit textEdit = retrieveTextEdit(textDocumentIdentifier, diagnostic, codeActions);
		Range range = textEdit.getRange();
		new RangeChecker().check(range, 9, 33, 9, 37);
		assertThat(textEdit.getNewText()).isEqualTo("delay");
	}

}
