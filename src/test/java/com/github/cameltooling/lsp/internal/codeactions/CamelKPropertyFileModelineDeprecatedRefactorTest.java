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

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKPropertyFileModelineDeprecatedRefactorTest extends AbstractCamelLanguageServerTest {

	@Test
	void testRefactor() throws Exception {
		testBasicRefactor("// camel-k: property-file=aFile.properties", "property=file:aFile.properties");
	}
	
	@Test
	void testRefactorWithEmptyValue() throws Exception {
		testBasicRefactor("// camel-k: property-file=", "property=file:");
	}
	
	@Test
	void testRefactorWithNoEqual() throws Exception {
		testBasicRefactor("// camel-k: property-file", "property=file:");
	}

	private void testBasicRefactor(String text, String expectedNewText) throws URISyntaxException, InterruptedException, ExecutionException {
		List<Either<Command, CodeAction>> codeActions = retrieveCodeActions(text);
		assertThat(codeActions).hasSize(1);
		CodeAction codeAction = codeActions.get(0).getRight();
		assertThat(codeAction).isNotNull();
		assertThat(codeAction.getTitle()).isEqualTo(ConvertCamelKPropertyFileModelineRefactorAction.CODE_ACTION_TITLE_CONVERT_PROPERTY_FILE);
		Map<String, List<TextEdit>> changes = codeAction.getEdit().getChanges();
		assertThat(changes).hasSize(1);
		List<TextEdit> textEdits = changes.get(DUMMY_URI+".groovy");
		assertThat(textEdits).hasSize(1);
		TextEdit textEdit = textEdits.get(0);
		assertThat(textEdit.getRange()).isEqualTo(new Range(new Position(0,12), new Position(0, text.length())));
		assertThat(textEdit.getNewText()).isEqualTo(expectedNewText);
	}
	
	@Test
	void testRefactorWithSeveralPropertyFileOptions() throws Exception {
		String text = "// camel-k: property-file=aFile1.properties property-file=aFile2.properties";
		List<Either<Command, CodeAction>> codeActions = retrieveCodeActions(text);
		assertThat(codeActions).hasSize(1);
		CodeAction codeAction = codeActions.get(0).getRight();
		assertThat(codeAction).isNotNull();
		assertThat(codeAction.getTitle()).isEqualTo(ConvertCamelKPropertyFileModelineRefactorAction.CODE_ACTION_TITLE_CONVERT_PROPERTY_FILE);
		Map<String, List<TextEdit>> changes = codeAction.getEdit().getChanges();
		assertThat(changes).hasSize(1);
		List<TextEdit> textEdits = changes.get(DUMMY_URI+".groovy");
		assertThat(textEdits).hasSize(2);
		
		TextEdit textEdit1 = textEdits.get(0);
		assertThat(textEdit1.getRange()).isEqualTo(new Range(new Position(0,12), new Position(0, 43)));
		assertThat(textEdit1.getNewText()).isEqualTo("property=file:aFile1.properties");
		
		TextEdit textEdit2 = textEdits.get(1);
		assertThat(textEdit2.getRange()).isEqualTo(new Range(new Position(0,44), new Position(0, text.length())));
		assertThat(textEdit2.getNewText()).isEqualTo("property=file:aFile2.properties");
	}

	private List<Either<Command, CodeAction>> retrieveCodeActions(String text) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".groovy");
		CodeActionContext context = new CodeActionContext(Collections.emptyList(), Collections.singletonList(CodeActionKind.Refactor));
		Range range = new Range(new Position(0,14), new Position(0, 14));
		CodeActionParams params = new CodeActionParams(new TextDocumentIdentifier(DUMMY_URI+".groovy"), range, context);
		return languageServer.getTextDocumentService().codeAction(params).get();
	}

}
