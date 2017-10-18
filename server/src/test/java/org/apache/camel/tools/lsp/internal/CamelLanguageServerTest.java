/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.tools.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.Test;


public class CamelLanguageServerTest {
	
	@Test
	public void testProvideDummyCompletion() throws Exception {
		InitializeParams params = new InitializeParams();
		params.setProcessId(new Random().nextInt());
		params.setRootUri(getTestResource("/workspace/").toURI().toString());
		CamelLanguageServer camelLanguageServer = new CamelLanguageServer();
		CompletableFuture<InitializeResult> initialize = camelLanguageServer.initialize(params);
		
		assertThat(initialize).isCompleted();
		assertThat(initialize.get().getCapabilities().getCompletionProvider().getResolveProvider()).isTrue();
		
		TextDocumentService textDocumentService = camelLanguageServer.getTextDocumentService();
		
		TextDocumentPositionParams dummyCompletionPositionRequest = new TextDocumentPositionParams(new TextDocumentIdentifier("dummy"), new Position(0, 0));
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = textDocumentService.completion(dummyCompletionPositionRequest);
		
		assertThat(completions.get().getLeft()).contains(new CompletionItem("dummyCamelCompletion"));
	}
	
	
	public File getTestResource(String name) throws URISyntaxException {
		return Paths.get(CamelLanguageServerTest.class.getResource(name).toURI()).toFile();
	}
}
