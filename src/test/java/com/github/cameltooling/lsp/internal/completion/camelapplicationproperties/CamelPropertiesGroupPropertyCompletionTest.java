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
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelPropertiesGroupPropertyCompletionTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideCompletion() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 11), "camel.main.");
		
		assertThat(completions.get().getLeft()).contains(createExpectedCompletionItem("allowUseOriginalMessage"));
	}
	
	@Test
	void testProvideCompletionWithDashedName() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 11), "camel.main.\ncamel.main.auto-startup=true");
		
		assertThat(completions.get().getLeft()).contains(createExpectedCompletionItem("allow-use-original-message"));
	}
	
	private CompletionItem createExpectedCompletionItem(String expectedOption) {
		CompletionItem completionItem = new CompletionItem(expectedOption);
		completionItem.setDocumentation("Sets whether to allow access to the original message from Camel's error handler, or from org.apache.camel.spi.UnitOfWork.getOriginalInMessage(). Turning this off can optimize performance, as defensive copy of the original message is not needed. Default is false.");
		completionItem.setDeprecated(false);
		completionItem.setInsertText(expectedOption + "=");
		return completionItem;
	}

	protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> retrieveCompletion(Position position, String text) throws URISyntaxException, InterruptedException, ExecutionException {
		String fileName = "a.properties";
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(".properties", new TextDocumentItem(fileName, CamelLanguageServer.LANGUAGE_ID, 0, text));
		return getCompletionFor(camelLanguageServer, position, fileName);
	}
}
