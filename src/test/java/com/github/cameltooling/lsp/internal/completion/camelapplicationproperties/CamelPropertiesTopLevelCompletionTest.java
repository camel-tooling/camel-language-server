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

import org.apache.camel.tooling.model.MainModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.google.gson.Gson;

class CamelPropertiesTopLevelCompletionTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testProvideCompletion() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 6));
		
		MainModel mainModel = loadMainModel(camelLanguageServer);
		
		assertThat(completions.get().getLeft()).hasSize(mainModel.getGroups().size() + 1);
	}
	
	@Test
	void testProvideCompletionOnSecondLine() throws Exception {
		String fileName = "a.properties";
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(fileName, new TextDocumentItem(fileName, CamelLanguageServer.LANGUAGE_ID, 0, "whateverfirstline\ncamel."));
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 6), fileName);
		
		MainModel mainModel = loadMainModel(camelLanguageServer);
		
		assertThat(completions.get().getLeft()).hasSize(mainModel.getGroups().size() + 1);
	}

	private MainModel loadMainModel(CamelLanguageServer camelLanguageServer) throws InterruptedException, ExecutionException {
		String mainJsonSchema = camelLanguageServer.getTextDocumentService().getCamelCatalog().get().mainJsonSchema();
		return new Gson().fromJson(mainJsonSchema, MainModel.class);
	}
	
	@Test
	void testProvideCompletionForCamel() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 0));
		
		assertThat(completions.get().getLeft().get(0).getLabel()).isEqualTo("camel.");
	}
	
	protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> retrieveCompletion(Position position) throws URISyntaxException, InterruptedException, ExecutionException {
		String fileName = "a.properties";
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(fileName, new TextDocumentItem(fileName, CamelLanguageServer.LANGUAGE_ID, 0, "camel.\ncamel."));
		return getCompletionFor(camelLanguageServer, position, fileName);
	}
}
