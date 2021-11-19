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
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKModelineTraitHoverTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideDocumentationForTraitDefinitionNameOnHover() throws Exception {
		testHover(20, "The Quarkus trait configures the Quarkus runtime. It's enabled by default. NOTE: Compiling to a native executable, i.e. when using `package-type=native`, is only supported for kamelets, as well as YAML and XML integrations. It also requires at least 4GiB of memory, so the Pod running the native build, that is either the operator Pod, or the build Pod (depending on the build strategy configured for the platform), must have enough memory available.");
	}
	
	@Test
	void testRangeForTraitDefinitionOnHover() throws Exception {
		Hover hover = testHover(20, "The Quarkus trait configures the Quarkus runtime. It's enabled by default. NOTE: Compiling to a native executable, i.e. when using `package-type=native`, is only supported for kamelets, as well as YAML and XML integrations. It also requires at least 4GiB of memory, so the Pod running the native build, that is either the operator Pod, or the build Pod (depending on the build strategy configured for the platform), must have enough memory available.");
		assertThat(hover.getRange()).isEqualTo(new Range(new Position(0, 18), new Position(0, 25)));
	}
	
	@Test
	void testProvideDocumentationForTraitPropertyNameOnHover() throws Exception {
		testHover(28, "Can be used to enable or disable a trait. All traits share this common property.");
	}
	
	@Test
	void testRangeForTraitPropertyNameOnHover() throws Exception {
		Hover hover = testHover(28, "Can be used to enable or disable a trait. All traits share this common property.");
		assertThat(hover.getRange()).isEqualTo(new Range(new Position(0, 26), new Position(0, 33)));
	}
	
	@Test
	void testProvideHoverForTraitPropetyNameWithPartialName() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=quarkus.enabled", ".java");
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".java"), new Position(0, 28));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo("Can be used to enable or disable a trait. All traits share this common property.");
	}
	
	@Test
	void testProvideHoverOnSecondLine() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("\n// camel-k: trait=quarkus.enabled", ".java");
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".java"), new Position(1, 28));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo("Can be used to enable or disable a trait. All traits share this common property.");
	}
	
	@Test
	void testProvideNoErroOnHoverOnDefinitionNameForUnknownDefinitionName() throws Exception {
		checkNoHover("// camel-k: trait=unknown.enabled=true", 20);
	}
	
	@Test
	void testProvideNoErroOnHoverOnPropertyForUnknownDefinitionName() throws Exception {
		checkNoHover("// camel-k: trait=unknown.enabled=true", 28);
	}
	
	@Test
	void testProvideNoErroOnHoverOnPropertyForUnknownDefintionName() throws Exception {
		checkNoHover("// camel-k: trait=quarkus.unknown=true", 28);
	}

	private void checkNoHover(String modeline, int character)
			throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(modeline, ".java");
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".java"), new Position(0, character));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get()).isNull();
	}
	
	private Hover testHover(int positionInLine, String expectedHoverContent)
			throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("// camel-k: trait=quarkus.enabled=true", ".java");
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".java"), new Position(0, positionInLine));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(expectedHoverContent);
		return hover.get();
	}
	
}
