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

class CamelKafkaConnectorCamelUrlRefactorTest extends AbstractCamelLanguageServerTest {

	@Test
	void testSimpleRefactorForSourceUrl() throws Exception {
		testConvertToListRefactor(
				"camel.source.url=timer:aName?daemon=true",
				new String[] {"camel.source.path.timerName=aName","camel.source.endpoint.daemon=true"});
	}
	
	@Test
	void testSimpleRefactorWithURlFinishingbyQuestionmark() throws Exception {
		testConvertToListRefactor(
				"camel.source.url=timer:aName?",
				new String[] {"camel.source.path.timerName=aName"});
	}
	
	@Test
	void testeRefactorWithEmptyoptionValue() throws Exception {
		testConvertToListRefactor(
				"camel.source.url=timer:aName?daemon=",
				new String[] {"camel.source.path.timerName=aName","camel.source.endpoint.daemon="});
	}
	
	@Test
	void testRefactorWithSeveralOptions() throws Exception {
		testConvertToListRefactor(
				"camel.source.url=timer:aName?daemon=true&delay=10s",
				new String[] {"camel.source.path.timerName=aName","camel.source.endpoint.daemon=true", "camel.source.endpoint.delay=10s"});
	}
	
	@Test
	void testRefactorWithSeveralPathParamForSourceUrl() throws Exception {
		testConvertToListRefactor(
				"camel.sink.url=activemq:test1:test2",
				new String[] {"camel.sink.path.destinationType=test1","camel.sink.path.destinationName=test2"});
	}

	private void testConvertToListRefactor(String text, String[] expected) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		CodeActionContext context = new CodeActionContext(Collections.emptyList(), Collections.singletonList(CodeActionKind.Refactor));
		Range range = new Range(new Position(0,0), new Position(0, 0));
		CodeActionParams params = new CodeActionParams(new TextDocumentIdentifier(DUMMY_URI+".properties"), range, context);
		List<Either<Command,CodeAction>> codeActions = languageServer.getTextDocumentService().codeAction(params).get();
		assertThat(codeActions).hasSize(1);
		CodeAction codeAction = codeActions.get(0).getRight();
		assertThat(codeAction).isNotNull();
		assertThat(codeAction.getTitle()).isEqualTo(ConvertCamelKafkaConnectorURLToPropertiesRefactorAction.CONVERT_TO_LIST_OF_PROPERTIES_NOTATION);
		Map<String, List<TextEdit>> changes = codeAction.getEdit().getChanges();
		assertThat(changes).hasSize(1);
		List<TextEdit> textEdits = changes.get(DUMMY_URI+".properties");
		assertThat(textEdits).hasSize(1);
		TextEdit textEdit = textEdits.get(0);
		assertThat(textEdit.getRange()).isEqualTo(new Range(new Position(0,0), new Position(0, text.length())));
		assertThat(textEdit.getNewText().split("\n")).containsExactlyInAnyOrder(expected);
	}

}
