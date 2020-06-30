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
package com.github.cameltooling.lsp.internal.completion.modeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKModelineTraitPropertyNameTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideCompletionAfterTraitDefinitionDot() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=quarkus.");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 26));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(2);
		CompletionItem completionItem = findCompletionItemWithLabel(completions, "native");
		assertThat(completionItem.getDocumentation().getLeft()).isEqualTo("The Quarkus runtime type (reserved for future use)");
		assertThat(completionItem.getInsertText()).isEqualTo("native=false");
	}
	
	@Test
	void testProvideCompletionWithPartialName() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=quarkus.na");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 28));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(1);
	}
	
	@Test
	void testProvideCompletionWithDefaultValueAString() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=container.");
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 28));
		CompletionItem completionItem = findCompletionItemWithLabel(completions, "port-name");
		assertThat(completionItem.getInsertText()).isEqualTo("port-name=http");
	}
	
	@Test
	void testProvideCompletionWithoutDefaultValue() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=container.");
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 28));
		CompletionItem completionItem = findCompletionItemWithLabel(completions, "request-cpu");
		assertThat(completionItem.getInsertText()).isNull();
	}
	
	@Test
	void testProvideCompletionWithDefaultValueAnumber() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=container.");
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 28));
		CompletionItem completionItem = findCompletionItemWithLabel(completions, "port");
		assertThat(completionItem.getInsertText()).isEqualTo("port=8080");
	}

	private CompletionItem findCompletionItemWithLabel(CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions, String label) throws InterruptedException, ExecutionException {
		List<CompletionItem> completionItems = completions.get().getLeft();
		Optional<CompletionItem> platformTraitCompletionItem = completionItems.stream()
				.filter(completionItem ->  label.equals(completionItem.getLabel()))
				.findFirst();
		
		assertThat(platformTraitCompletionItem).isNotNull();
		return platformTraitCompletionItem.get();
	}
	
	@Test
	void testProvideNoCompletionForUnknownTraitDefinitionName() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=unknown.");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 26));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(0);
	}
	
}
