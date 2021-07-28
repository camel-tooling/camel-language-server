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
package com.github.cameltooling.lsp.internal.folding;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class FoldingRangeRouteTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testFoldingRangeOnJavaFile() throws Exception {
		File file = new File("src/test/resources/workspace with space/MyRouteBuilder.java");
		CamelLanguageServer languageServer = initializeLanguageServer(file);
		
		List<FoldingRange> foldingRanges = getFoldingRanges(file, languageServer).get();
		
		assertThat(foldingRanges).hasSize(1);
		FoldingRange foldingRange = foldingRanges.get(0);
		assertThat(foldingRange).isEqualTo(new FoldingRange(15, 20));
	}
	
	@Test
	void testMultipleFoldingRangeOnJavaFile() throws Exception {
		File file = new File("src/test/resources/workspace/My3RoutesBuilder.java");
		CamelLanguageServer languageServer = initializeLanguageServer(file);
		
		List<FoldingRange> foldingRanges = getFoldingRanges(file, languageServer).get();
		
		assertThat(foldingRanges).hasSize(3);
	}
	
	@Test
	void testNoFoldingRangeOnJavaFile() throws Exception {
		File file = new File("src/test/resources/workspace/AnInterface.java");
		CamelLanguageServer languageServer = initializeLanguageServer(file);
		
		List<FoldingRange> foldingRanges = getFoldingRanges(file, languageServer).get();
		
		assertThat(foldingRanges).isEmpty();
	}
	
	@Test
	void testNoFoldingRangeOnInvalidJavaFile() throws Exception {
		File file = new File("src/test/resources/workspace/AnInvalid.java");
		CamelLanguageServer languageServer = initializeLanguageServer(file);
		
		List<FoldingRange> foldingRanges = getFoldingRanges(file, languageServer).get();
		
		assertThat(foldingRanges).isEmpty();
	}

	private CompletableFuture<List<FoldingRange>> getFoldingRanges(File file, CamelLanguageServer languageServer) {
		TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier(file.toURI().toString());
		FoldingRangeRequestParams params = new FoldingRangeRequestParams(textDocumentIdentifier);
		return languageServer.getTextDocumentService().foldingRange(params);
	}
	
}
