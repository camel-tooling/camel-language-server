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
package com.github.cameltooling.lsp.internal.completion.camelapplicationproperties;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKafkaConnectorClassCompletionTest extends AbstractCamelKafkaConnectorTest {

	@Test
	void testProvideCompletion() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 16), "connector.class=");
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		String connectorClassName = "org.test.kafkaconnector.TestSourceConnector";
		CompletionItem completionItem = completionItems.stream().filter(ci -> connectorClassName.equals(ci.getLabel())).findAny().get();
		assertThat(completionItem.getTextEdit().getNewText()).isEqualTo(connectorClassName);
		assertThat(completionItem.getTextEdit().getRange()).isEqualTo(new Range(new Position(0, 16), new Position(0, 16)));
	}
	
	@Test
	void testFilterCompletion() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 44), "connector.class=org.test.kafkaconnector.TestSinkConnector");
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(2);
		String connectorClassName = "org.test.kafkaconnector.TestSinkConnector";
		CompletionItem completionItem = completionItems.stream().filter(ci -> connectorClassName.equals(ci.getLabel())).findAny().get();
		assertThat(completionItem.getTextEdit().getNewText()).isEqualTo(connectorClassName);
		assertThat(completionItem.getTextEdit().getRange()).isEqualTo(new Range(new Position(0, 16), new Position(0, 57)));
	}
	
	protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> retrieveCompletion(Position position, String text) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(text);
		return getCompletionFor(camelLanguageServer, position);
	}
}
