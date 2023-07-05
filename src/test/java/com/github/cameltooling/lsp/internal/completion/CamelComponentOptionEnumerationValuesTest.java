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
package com.github.cameltooling.lsp.internal.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.List;
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
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

class CamelComponentOptionEnumerationValuesTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideEnumValues() throws Exception {
		testProvideCamelOptions(RouteTextBuilder.createXMLBlueprintRoute("timer:timerName?exchangePattern="), 0, 43);
	}

	private void testProvideCamelOptions(String textTotest, int line, int character) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textTotest);

		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(line, character));

		assertThat(completions.get().getLeft()).contains(
				createExpectedCompletionItem("InOnly"),
				createExpectedCompletionItem("InOut"));
	}

	private CompletionItem createExpectedCompletionItem(String enumOption) {
		CompletionItem completionItem = new CompletionItem(enumOption);
		completionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(0, 43), new Position(0, 43)), enumOption)));
		return completionItem;
	}

}
