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

import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelPropertiesFileHoverTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testHoverOnCamelMainOptions() throws Exception {
		String propertyEntry = "camel.main.backlogTracing=true";
		CompletableFuture<Hover> hover = getHover(propertyEntry);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo("Sets whether backlog tracing is enabled or not. Default is false.");
	}
	
	@Test
	void testNoHoverOnUnknownCamelMainOptions() throws Exception {
		String propertyEntry = "camel.main.unknown=true";
		CompletableFuture<Hover> hover = getHover(propertyEntry);
		
		assertThat(hover.get()).isNull();
	}
	
	private CompletableFuture<Hover> getHover(String propertyEntry) throws URISyntaxException, InterruptedException, ExecutionException {
		String fileName = "a.properties";
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(fileName, new TextDocumentItem(fileName, CamelLanguageServer.LANGUAGE_ID, 0, propertyEntry));
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(fileName), new Position(0, 13));
		return camelLanguageServer.getTextDocumentService().hover(hoverParams);
	}

}
