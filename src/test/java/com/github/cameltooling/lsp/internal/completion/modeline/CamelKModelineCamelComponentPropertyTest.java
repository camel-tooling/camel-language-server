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

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKModelineCamelComponentPropertyTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideCompletionForComponentNames() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: property=camel.component.");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 37));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSizeGreaterThan(50);
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> "timer".equals(completionItem.getLabel())).findFirst().get();
		assertThat(timerCompletionItem.getDocumentation().getLeft()).isEqualTo("Generate messages in specified intervals using java.util.Timer.");
	}
	
	@Test
	void testProvideCompletionForComponentNamesWithInsertAndReplace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: property=camel.component.toreplace");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 37));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSizeGreaterThan(50);
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> "timer".equals(completionItem.getLabel())).findFirst().get();
		assertThat(timerCompletionItem.getDocumentation().getLeft()).isEqualTo("Generate messages in specified intervals using java.util.Timer.");
		TextEdit textEdit = timerCompletionItem.getTextEdit().getLeft();
		assertThat(textEdit).isNotNull();
		assertThat(textEdit.getNewText()).isEqualTo("timer");
		assertThat(textEdit.getRange().getStart().getCharacter()).isEqualTo(37);
		assertThat(textEdit.getRange().getEnd().getCharacter()).isEqualTo(37 + "toreplace".length());
	}
	
	@Test
	void testProvideCompletionForGroup() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: property=camel.");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 27));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		CompletionItem faultToleranceCompletionItem = completionItems.stream().filter(completionItem -> "faulttolerance".equals(completionItem.getLabel())).findFirst().get();
		assertThat(faultToleranceCompletionItem).isNotNull();
		assertThat(faultToleranceCompletionItem.getTextEdit().getLeft().getNewText()).isEqualTo("faulttolerance.");
		
		CompletionItem componentCompletionItem = completionItems.stream().filter(completionItem -> "component".equals(completionItem.getLabel())).findFirst().get();
		assertThat(componentCompletionItem).isNotNull();
		assertThat(componentCompletionItem.getTextEdit().getLeft().getNewText()).isEqualTo("component.");
	}
	
	@Test
	void testProvideCompletionForGroupWithInsertAndReplaceWithPArtialProperty() throws Exception {
		testProvideCompletionForGroupWithInsertAndReplace("// camel-k: property=camel.toreplace", true);
	}
	
	@Test
	void testProvideCompletionForGroupWithInsertAndReplace() throws Exception {
		testProvideCompletionForGroupWithInsertAndReplace("// camel-k: property=camel.toreplace.property=value", false);
	}

	private void testProvideCompletionForGroupWithInsertAndReplace(String modeline, boolean withFinalDot) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(modeline);
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 27));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		CompletionItem faultToleranceCompletionItem = completionItems.stream().filter(completionItem -> "faulttolerance".equals(completionItem.getLabel())).findFirst().get();
		assertThat(faultToleranceCompletionItem).isNotNull();
		TextEdit faulToleranceTextEdit = faultToleranceCompletionItem.getTextEdit().getLeft();
		assertThat(faulToleranceTextEdit.getNewText()).isEqualTo("faulttolerance" + (withFinalDot? "." : ""));
		assertThat(faulToleranceTextEdit.getRange().getStart().getCharacter()).isEqualTo(27);
		assertThat(faulToleranceTextEdit.getRange().getEnd().getCharacter()).isEqualTo(27 + "toreplace".length());
		
		CompletionItem componentCompletionItem = completionItems.stream().filter(completionItem -> "component".equals(completionItem.getLabel())).findFirst().get();
		assertThat(componentCompletionItem).isNotNull();
		assertThat(componentCompletionItem.getTextEdit().getLeft().getNewText()).isEqualTo("component" + (withFinalDot? "." : ""));
		TextEdit componentTextEdit = componentCompletionItem.getTextEdit().getLeft();
		assertThat(componentTextEdit.getNewText()).isEqualTo("component" + (withFinalDot? "." : ""));
		assertThat(componentTextEdit.getRange().getStart().getCharacter()).isEqualTo(27);
		assertThat(componentTextEdit.getRange().getEnd().getCharacter()).isEqualTo(27 + "toreplace".length());
	}
	
	@Test
	void testProvideCompletionForComponentAttribute() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: property=camel.component.timer.");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 43));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(3);
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> "bridgeErrorHandler".equals(completionItem.getLabel())).findFirst().get();
		assertThat(timerCompletionItem.getDocumentation().getLeft()).isEqualTo("Allows for bridging the consumer to the Camel routing Error Handler, which mean any exceptions (if possible) occurred while the Camel consumer is trying to pickup incoming messages, or the likes, will now be processed as a message and handled by the routing Error Handler. Important: This is only possible if the 3rd party component allows Camel to be alerted if an exception was thrown. Some components handle this internally only, and therefore bridgeErrorHandler is not possible. In other situations we may improve the Camel component to hook into the 3rd party component and make this possible for future releases. By default the consumer will use the org.apache.camel.spi.ExceptionHandler to deal with exceptions, that will be logged at WARN or ERROR level and ignored.");
	}
	
	@Test
	void testProvideCompletionForComponentAttributeWithDashedNotation() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: property=camel.component.timer. property=camel.component.timer.basic-property-binding=true");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 43));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).hasSize(3);
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> "bridge-error-handler".equals(completionItem.getLabel())).findFirst().get();
		assertThat(timerCompletionItem.getDocumentation().getLeft()).isEqualTo("Allows for bridging the consumer to the Camel routing Error Handler, which mean any exceptions (if possible) occurred while the Camel consumer is trying to pickup incoming messages, or the likes, will now be processed as a message and handled by the routing Error Handler. Important: This is only possible if the 3rd party component allows Camel to be alerted if an exception was thrown. Some components handle this internally only, and therefore bridgeErrorHandler is not possible. In other situations we may improve the Camel component to hook into the 3rd party component and make this possible for future releases. By default the consumer will use the org.apache.camel.spi.ExceptionHandler to deal with exceptions, that will be logged at WARN or ERROR level and ignored.");
	}
		
	@Test
	void testProvideCompletionForComponentSuffixWithInseertAndReplace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: property=test.test");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 21));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		Optional<CompletionItem> timerCompletionItem = completionItems.stream().filter(completionItem -> "camel".equals(completionItem.getLabel())).findFirst();
		assertThat(timerCompletionItem).isNotEmpty();
		TextEdit timerTextEdit = timerCompletionItem.get().getTextEdit().getLeft();
		assertThat(timerTextEdit).isNotNull();
		assertThat(timerTextEdit.getNewText()).isEqualTo("camel");
		Range timerTextEditRange = timerTextEdit.getRange();
		assertThat(timerTextEditRange.getStart().getCharacter()).isEqualTo(21);
		assertThat(timerTextEditRange.getEnd().getCharacter()).isEqualTo(21+"test".length());
	}
	
}
