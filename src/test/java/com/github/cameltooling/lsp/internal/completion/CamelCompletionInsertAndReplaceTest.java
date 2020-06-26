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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelCompletionInsertAndReplaceTest extends AbstractCamelLanguageServerTest {

	@Test
	void testComponent() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"ahc-wss:httpUri?binding=#true&amp;bufferSize=10&amp;synchronous=true\" xmlns=\"http://camel.apache.org/schema/blueprint\"/>\n", ".xml");
		Position positionInMiddleOfcomponentPart = new Position(0, 15);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, positionInMiddleOfcomponentPart);
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items).hasSize(2);
		for (CompletionItem completionItem : items) {
			TextEdit textEdit = completionItem.getTextEdit();
			Range range = textEdit.getRange();
			assertThat(range.getStart().getLine()).isZero();
			assertThat(range.getStart().getCharacter()).isEqualTo(11 /*start of URI */);
			assertThat(range.getEnd().getLine()).isZero();
			assertThat(range.getEnd().getCharacter()).isEqualTo(26 /* just before the '?' */);
		}
	}
	
	@Test
	void testAttribute() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"ahc-wss:httpUri?binding=#true&amp;bufferSize=10&amp;synchronous=true\" xmlns=\"http://camel.apache.org/schema/blueprint\"/>\n", ".xml");
		Position positionBeforeBufferSizeAttribute = new Position(0, 45);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, positionBeforeBufferSizeAttribute);
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items).hasSize(14);
		for (CompletionItem completionItem : items) {
			TextEdit textEdit = completionItem.getTextEdit();
			Range range = textEdit.getRange();
			assertThat(range.getStart().getLine()).isZero();
			assertThat(range.getStart().getCharacter()).isEqualTo(45 /* just before 'bufferSize' */);
			assertThat(range.getEnd().getLine()).isZero();
			assertThat(range.getEnd().getCharacter()).isEqualTo(55 /* end of 'bufferSize' */);
		}
	}
}
