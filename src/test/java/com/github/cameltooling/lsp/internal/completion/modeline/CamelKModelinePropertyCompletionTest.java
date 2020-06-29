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
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKModelinePropertyCompletionTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideCompletionWithOnlyPrefix() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: dependency=");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 23));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).isNotEmpty();
		assertThat(completionItems.stream().map(completionitem -> completionitem.getLabel())).allMatch(compleItemLabel -> compleItemLabel.startsWith("camel-"));
		CompletionItem timerCompletionItem = completionItems.stream().filter(completionItem -> "camel-timer".equals(completionItem.getLabel())).findFirst().get();
		assertThat(timerCompletionItem.getDocumentation().getLeft()).isEqualTo("Generate messages in specified intervals using java.util.Timer.");
	}
}
