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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

class CamelPropertiesComponentOptionNameCompletionTest extends AbstractCamelPropertiesComponentOptionTest {
	
	@Test
	void testProvideCompletion() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.");
		
		assertThat(completions.get().getLeft()).hasSize(2);
	}
	
	@Test
	void testProvideCompletionHasDefaultValue() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.");
		
		assertThat(completions.get().getLeft().get(0).getInsertText()).isEqualTo("aComponentProperty=aDefaultValue");
	}
	
	@Test
	void testProvideCompletionWithoutDefaultValueIfAValueAlreadyProvided() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.aComponentProperty=aValue");
		CompletionItem expectedCompletionItem = new CompletionItem("aComponentProperty");
		expectedCompletionItem.setInsertText("aComponentProperty");
		expectedCompletionItem.setDocumentation("A parameter description");
		expectedCompletionItem.setDeprecated(false);
		expectedCompletionItem.setDetail(String.class.getName());
		expectedCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(0, 27), new Position(0, 45)), "aComponentProperty")));
		assertThat(completions.get().getLeft()).contains(expectedCompletionItem);
	}
	
	@Test
	void testInsertAndReplace() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.awrongtoreplace=aValue");
		CompletionItem expectedCompletionItem = new CompletionItem("aComponentProperty");
		expectedCompletionItem.setInsertText("aComponentProperty");
		expectedCompletionItem.setDocumentation("A parameter description");
		expectedCompletionItem.setDeprecated(false);
		expectedCompletionItem.setDetail(String.class.getName());
		expectedCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(0, 27), new Position(0, 42)), "aComponentProperty")));
		assertThat(completions.get().getLeft()).contains(expectedCompletionItem);
	}
	
	@Test
	void testProvideNoCompletionAfterComponentproperty() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 46), "camel.component.acomponent.aComponentProperty.");
		
		assertThat(completions.get().getLeft()).isEmpty();
	}
	
	@Test
	void testProvideFilteredCompletionWhenInsideValue() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 29), "camel.component.acomponent.aComponentProperty.");
		
		assertThat(completions.get().getLeft()).hasSize(1);
		assertThat(completions.get().getLeft().get(0).getInsertText()).isEqualTo("aComponentProperty=aDefaultValue");
	}
}
