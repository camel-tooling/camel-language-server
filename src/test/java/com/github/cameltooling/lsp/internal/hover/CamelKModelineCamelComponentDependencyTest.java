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
package com.github.cameltooling.lsp.internal.hover;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKModelineCamelComponentDependencyTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideDocumentationOnHover() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: dependency=camel-timer", ".java");
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".java"), new Position(0, 24));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo("Generate messages in specified intervals using java.util.Timer.");
	}
	
	@Test
	void testNoHoverOnNonCamelDependency() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: dependency=unknown", ".java");
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".java"), new Position(0, 24));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get()).isNull();
	}
}
