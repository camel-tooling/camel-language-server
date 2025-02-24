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
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKModelineCompletionTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideCompletionWithOnlyPrefix() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: ");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 12));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(12);
		checkTraitCompletionAvailable(completionItems);
	}
	
	@Test
	void testProvideCompletionOnSecondLine() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("\n// camel-k: ");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 12));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(12);
		checkTraitCompletionAvailable(completionItems);
	}
	
	@Test
	void testProvideCompletionWithPartialName() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: tr");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 14));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(1);
		checkTraitCompletionAvailable(completionItems);
	}
	
	private void checkTraitCompletionAvailable(List<CompletionItem> completionItems) {
		CompletionItem traitCompletionItem = new CompletionItem("trait");
		traitCompletionItem.setDocumentation("Configure a trait. E.g. \"trait=service.enabled=false\"");
		assertThat(completionItems).contains(traitCompletionItem);
	}
	
	@Test
	void testProvideCompletionAtTheEndOfLine() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("# camel-k: language=yaml trait=service.enabled=false ");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 53));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(12);
		checkTraitCompletionAvailable(completionItems);
	}
	
	@Test
	void testProvideNoCompletionInPrefix() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: ");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 5));
		
		assertThat(completions.get().getLeft()).isEmpty();
	}
	
	@Test
	void testPropertyFileOptionIsDeprecated() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: property-file");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 23));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(1);
		CompletionItem completionItem = completionItems.get(0);
		assertThat(completionItem.getLabel()).isEqualTo("property-file");
		assertThat(completionItem.getTags()).contains(CompletionItemTag.Deprecated);
	}
	
}
