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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKModelineTraitDefinitionNameTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideCompletionAfterEqual() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 18));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(28);
		Optional<CompletionItem> platformTraitCompletionItem = completionItems.stream()
				.filter(completionItem ->  "platform".equals(completionItem.getLabel()))
				.findFirst();
		
		assertThat(platformTraitCompletionItem).isNotNull();
		assertThat(platformTraitCompletionItem.get().getDocumentation().getLeft()).isEqualTo("The platform trait is a base trait that is used to assign an integration platform to an integration.In case the platform is missing, the trait is allowed to create a default platform.This feature is especially useful in contexts where there's no need to provide a custom configuration for the platform(e.g. on OpenShift the default settings work, since there's an embedded container image registry).");
	}
	
	@Test
	void testProvideCompletionWithPartialName() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=pla");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 21));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(1);
		Optional<CompletionItem> platformTraitCompletionItem = completionItems.stream()
				.filter(completionItem ->  "platform".equals(completionItem.getLabel()))
				.findFirst();
		
		assertThat(platformTraitCompletionItem).isNotNull();
		assertThat(platformTraitCompletionItem.get().getDocumentation().getLeft()).isEqualTo("The platform trait is a base trait that is used to assign an integration platform to an integration.In case the platform is missing, the trait is allowed to create a default platform.This feature is especially useful in contexts where there's no need to provide a custom configuration for the platform(e.g. on OpenShift the default settings work, since there's an embedded container image registry).");
	}
	
	@Test
	void testProvideNoCompletionForNotATrait() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: other=");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 18));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).isEmpty();
	}
	
}
