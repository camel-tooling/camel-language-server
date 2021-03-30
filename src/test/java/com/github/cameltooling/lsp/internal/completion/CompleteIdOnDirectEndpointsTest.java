/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.github.cameltooling.lsp.internal.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

/**
 * @author lheinema
 */
class CompleteIdOnDirectEndpointsTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testDirectEndpointCompletion() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(new FileInputStream("src/test/resources/workspace/direct-endpoint-test.xml"), ".xml");
		Position positionInMiddleOfcomponentPart = new Position(9, 42);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, positionInMiddleOfcomponentPart);
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items).hasSize(2);
		for (CompletionItem completionItem : items) {
			TextEdit textEdit = completionItem.getTextEdit().getLeft();
			assertThat(textEdit.getNewText()).isIn("direct:name", "direct:processing");
		}
	}

	@Test
	void testDirectVMEndpointCompletion() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(new FileInputStream("src/test/resources/workspace/direct-endpoint-test.xml"), ".xml");
		Position positionInMiddleOfcomponentPart = new Position(17, 45);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, positionInMiddleOfcomponentPart);
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items).hasSize(2);
		for (CompletionItem completionItem : items) {
			TextEdit textEdit = completionItem.getTextEdit().getLeft();
			assertThat(textEdit.getNewText()).isIn("direct-vm:name", "direct-vm:processing");
		}
	}
	
	@Test
	void testVMEndpointCompletion() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(new FileInputStream("src/test/resources/workspace/direct-endpoint-test.xml"), ".xml");
		Position positionInMiddleOfcomponentPart = new Position(25, 39);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, positionInMiddleOfcomponentPart);
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items).hasSize(2);
		for (CompletionItem completionItem : items) {
			TextEdit textEdit = completionItem.getTextEdit().getLeft();
			assertThat(textEdit.getNewText()).isIn("vm:name", "vm:processing");
		}
	}
	
	@Test
	void testSEDAEndpointCompletion() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(new FileInputStream("src/test/resources/workspace/direct-endpoint-test.xml"), ".xml");
		Position positionInMiddleOfcomponentPart = new Position(33, 41);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, positionInMiddleOfcomponentPart);
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items).hasSize(2);
		for (CompletionItem completionItem : items) {
			TextEdit textEdit = completionItem.getTextEdit().getLeft();
			assertThat(textEdit.getNewText()).isIn("seda:name", "seda:processing");
		}
	}
}
