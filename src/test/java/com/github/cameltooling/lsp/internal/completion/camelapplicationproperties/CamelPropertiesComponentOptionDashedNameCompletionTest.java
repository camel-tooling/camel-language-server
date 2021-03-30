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

class CamelPropertiesComponentOptionDashedNameCompletionTest extends AbstractCamelPropertiesComponentOptionTest {
	
	private static String LINE_WITH_DASHED_COMPONENT = "\ncamel.component.acomponent.with-dash=demo";
	
	@Test
	void testProvideCompletion() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent." + LINE_WITH_DASHED_COMPONENT);
		
		assertThat(completions.get().getLeft()).hasSize(2);
	}
	
	@Test
	void testProvideCompletionHasDefaultValue() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent." + LINE_WITH_DASHED_COMPONENT);
		
		assertThat(completions.get().getLeft().get(0).getInsertText()).isEqualTo("a-component-property=aDefaultValue");
	}
	
	@Test
	void testProvideCompletionWithoutDefaultValueIfAValueAlreadyProvided() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.a-component-property=aValue");
		CompletionItem expectedCompletionItem = new CompletionItem("a-component-property");
		expectedCompletionItem.setInsertText("a-component-property");
		expectedCompletionItem.setDocumentation("A parameter description");
		expectedCompletionItem.setDeprecated(false);
		expectedCompletionItem.setDetail(String.class.getName());
		expectedCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(0, 27), new Position(0, 47)), "a-component-property")));
		assertThat(completions.get().getLeft()).contains(expectedCompletionItem);
	}
	
	@Test
	void testInsertAndReplace() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.a-wrong-to-replace=aValue");
		CompletionItem expectedCompletionItem = new CompletionItem("a-component-property");
		expectedCompletionItem.setInsertText("a-component-property");
		expectedCompletionItem.setDocumentation("A parameter description");
		expectedCompletionItem.setDeprecated(false);
		expectedCompletionItem.setDetail(String.class.getName());
		expectedCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(0, 27), new Position(0, 45)), "a-component-property")));
		assertThat(completions.get().getLeft()).contains(expectedCompletionItem);
	}
	
	@Test
	void testProvideNoCompletionAfterComponentproperty() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 48), "camel.component.acomponent.a-component-property.");
		
		assertThat(completions.get().getLeft()).isEmpty();
	}
	
	@Test
	void testProvideFilteredCompletionWhenInsideValue() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 30), "camel.component.acomponent.a-component-property.");
		
		assertThat(completions.get().getLeft()).hasSize(1);
		assertThat(completions.get().getLeft().get(0).getInsertText()).isEqualTo("a-component-property=aDefaultValue");
	}
}
