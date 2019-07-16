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
package com.github.cameltooling.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

public class CamelTextDocumentServiceTest extends AbstractCamelLanguageServerTest {
	
	@Test
	public void testChangeEventUpdatesStoredText() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<to uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n");
		
		DidChangeTextDocumentParams changeEvent = new DidChangeTextDocumentParams();
		VersionedTextDocumentIdentifier textDocument = new VersionedTextDocumentIdentifier();
		textDocument.setUri(DUMMY_URI+".xml");
		changeEvent.setTextDocument(textDocument);
		TextDocumentContentChangeEvent contentChange = new TextDocumentContentChangeEvent("<to xmlns=\"http://camel.apache.org/schema/blueprint\" uri=\"\"></to>\n");
		changeEvent.setContentChanges(Collections.singletonList(contentChange));
		camelLanguageServer.getTextDocumentService().didChange(changeEvent);
		
		//check old position doesn't provide completion
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completionsAtOldPosition = getCompletionFor(camelLanguageServer, new Position(0, 11));
		assertThat(completionsAtOldPosition.get().getLeft()).isEmpty();
		
		//check new position provides completion
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completionsAtNewPosition = getCompletionFor(camelLanguageServer, new Position(0, 58));
		assertThat(completionsAtNewPosition.get().getLeft()).isNotEmpty();
		
	}
}
