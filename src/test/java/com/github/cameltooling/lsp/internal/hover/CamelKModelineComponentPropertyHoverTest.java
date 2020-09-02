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
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKModelineComponentPropertyHoverTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideDocumentationOnHoverOfComponentName() throws Exception {
		testProvideDocumentationOnHover("// camel-k: property=camel.component.timer.basicPropertyBinding=true", 38, "Generate messages in specified intervals using java.util.Timer.");
	}
	
	@Test
	void testProvideDocumentationOnHoverOfComponentAttribute() throws Exception {
		testProvideDocumentationOnHover("// camel-k: property=camel.component.timer.basicPropertyBinding=true", 50, "Whether the component should use basic property binding (Camel 2.x) or the newer property binding with additional capabilities");
	}
	
	@Test
	void testProvideDocumentationOnHoverOfComponentAttributeUsingDashedName() throws Exception {
		testProvideDocumentationOnHover("// camel-k: property=camel.component.timer.basic-property-binding=true", 50, "Whether the component should use basic property binding (Camel 2.x) or the newer property binding with additional capabilities");
	}
	
	@Test
	void testNoErrorWithPartialNoComponentNameSpecified() throws Exception {
		testProvideDocumentationOnHover("// camel-k: property=camel.component.timer", 38, "Generate messages in specified intervals using java.util.Timer.");
	}
	
	@Test
	void testNoErrorWithUnknownComponent() throws Exception {
		testProvideDocumentationOnHover("// camel-k: property=camel.component.unknown", 38, null);
	}
	
	@Test
	void testNoErrorWithUnknownParameter() throws Exception {
		testProvideDocumentationOnHover("// camel-k: property=camel.component.timer.unknown", 50, null);
	}
	
	private void testProvideDocumentationOnHover(String modeline, int position, String expectedDescription) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(modeline, ".java");
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".java"), new Position(0, position));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		if (expectedDescription != null) {
			assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(expectedDescription);
		} else {
			assertThat(hover.get()).isNull();
		}
	}
}
