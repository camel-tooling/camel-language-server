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
import java.util.function.Predicate;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKModelineDependencyCompletionTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideCompletionForCamelComponentDependency() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: dependency=");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 23));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).isNotEmpty();
		
		assertThat(completionItems.stream().filter(isNotMvnOrJitPack()).map(completionitem -> completionitem.getLabel())).allMatch(compleItemLabel -> compleItemLabel.startsWith("camel-"));
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> "camel-timer".equals(completionItem.getLabel())).findFirst().get();
		assertThat(timerCompletionItem.getDocumentation().getLeft()).isEqualTo("Generate messages in specified intervals using java.util.Timer.");
		assertThat(timerCompletionItem.getTextEdit()).isNotNull();
		
	}

	private Predicate<? super CompletionItem> isNotMvnOrJitPack() {
		return completionItem -> !completionItem.getLabel().startsWith("mvn") && !completionItem.getLabel().startsWith("jitpack");
	}
		
	@Test
	void testProvideCompletionForCamelComponentDependencyOnSecondLine() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("\n// camel-k: dependency=");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 23));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).isNotEmpty();
		assertThat(completionItems.stream().filter(isNotMvnOrJitPack()).map(completionitem -> completionitem.getLabel())).allMatch(compleItemLabel -> compleItemLabel.startsWith("camel-"));
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> "camel-timer".equals(completionItem.getLabel())).findFirst().get();
		assertThat(timerCompletionItem.getDocumentation().getLeft()).isEqualTo("Generate messages in specified intervals using java.util.Timer.");
		assertThat(timerCompletionItem.getTextEdit().getRange().getStart().getLine()).isEqualTo(1);
	}
	
	@Test
	void testProvideCompletionWithInsertAndReplaceForCamelComponentDependency() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: dependency=camel-example");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 23));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).isNotEmpty();
		assertThat(completionItems.stream().filter(isNotMvnOrJitPack()).map(completionitem -> completionitem.getLabel())).allMatch(compleItemLabel -> compleItemLabel.startsWith("camel-"));
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> "camel-timer".equals(completionItem.getLabel())).findFirst().get();
		assertThat(timerCompletionItem.getDocumentation().getLeft()).isEqualTo("Generate messages in specified intervals using java.util.Timer.");
		TextEdit camelTimerTextEdit = timerCompletionItem.getTextEdit();
		assertThat(camelTimerTextEdit).isNotNull();
		assertThat(camelTimerTextEdit.getNewText()).isEqualTo("camel-timer");
		assertThat(camelTimerTextEdit.getRange().getStart().getCharacter()).isEqualTo(23);
		assertThat(camelTimerTextEdit.getRange().getEnd().getCharacter()).isEqualTo(23 + "camel-example".length());
	}
	
	@Test
	void testProvideCompletionWithInsertAndReplaceInMiddleOfValue() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: dependency=camel-tika");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 31));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(4/*camel-timer, camel-tika, mvn and jitpack completion */);
		assertThat(completionItems.stream().filter(isNotMvnOrJitPack()).map(completionitem -> completionitem.getLabel())).allMatch(compleItemLabel -> compleItemLabel.startsWith("camel-"));
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> "camel-timer".equals(completionItem.getLabel())).findFirst().get();
		assertThat(timerCompletionItem.getDocumentation().getLeft()).isEqualTo("Generate messages in specified intervals using java.util.Timer.");
		TextEdit camelTimerTextEdit = timerCompletionItem.getTextEdit();
		assertThat(camelTimerTextEdit).isNotNull();
		assertThat(camelTimerTextEdit.getNewText()).isEqualTo("camel-timer");
		assertThat(camelTimerTextEdit.getRange().getStart().getCharacter()).isEqualTo(23);
		assertThat(camelTimerTextEdit.getRange().getEnd().getCharacter()).isEqualTo(23 + "camel-tika".length());
	}
	
	@Test
	void testProvideCompletionForMavenComponentDependency() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: dependency=test");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 23));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).isNotEmpty();
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> completionItem.getLabel().startsWith("mvn")).findFirst().get();
		assertThat(timerCompletionItem.getInsertTextFormat()).isEqualTo(InsertTextFormat.Snippet);
		assertThat(timerCompletionItem.getInsertText()).isEqualTo("mvn:${1:groupId}/${2:artifactId}:${3:version}");
		assertThat(timerCompletionItem.getTextEdit().getRange().getStart().getCharacter()).isEqualTo(23);
		assertThat(timerCompletionItem.getTextEdit().getRange().getEnd().getCharacter()).isEqualTo(23 + "test".length());
	}
	
	@Test
	void testProvideCompletionForJitpackComponentDependency() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: dependency=test");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 23));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).isNotEmpty();
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> completionItem.getLabel().startsWith("jitpack")).findFirst().get();
		assertThat(timerCompletionItem.getInsertTextFormat()).isEqualTo(InsertTextFormat.Snippet);
		assertThat(timerCompletionItem.getInsertText()).isEqualTo("jitpack:${1|com.github,com.gitlab,com.bitbucket,com.gitee,com.azure|}.${2:username}:${3:repo}:${4:master-SNAPSHOT}");
		assertThat(timerCompletionItem.getTextEdit().getRange().getStart().getCharacter()).isEqualTo(23);
		assertThat(timerCompletionItem.getTextEdit().getRange().getEnd().getCharacter()).isEqualTo(23 + "test".length());
	}
}
