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

import static com.github.cameltooling.lsp.internal.util.RouteTextBuilder.createXMLSpringRoute;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.instancemodel.OptionParamURIInstance;

class CamelLanguageServerHoverTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testProvideDocumentationOnHover() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("timer:timerName"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 13));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(TIMER_DOCUMENTATION);
	}
	
	@Test
	void testRangeForCamelComponentOnHover() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("timer:timerName"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 13));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getRange()).isEqualTo(new Range(new Position(0, 11), new Position(0, 16)));
	}
	
	@Test
	void testProvideDocumentationOnHoverWithCamelPrefix() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<camel:from uri=\"timer:timerName\" xmlns:camel=\"http://camel.apache.org/schema/spring\"></camel:from>\n");
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 19));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(TIMER_DOCUMENTATION);
	}
	
	@Test
	void testProvideDocumentationOnHoverForJava() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(
				"//camel file\n"
				+ "from(\"timer:timerName\")",
				".java");
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".java"), new Position(1, 7));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(TIMER_DOCUMENTATION);
	}
	
	@Test
	void testProvideDocumentationOnHoverForJavaWithModeline() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(
				"// camel-k : trait=quarkus.enabled=true\n"
				+ "from(\"timer:timerName\")",
				".java");
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".java"), new Position(1, 7));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(TIMER_DOCUMENTATION);
	}
	
	@Test
	void testDontProvideDocumentationOnHoverForBadPlaces() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("timer:timerName"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 5));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get()).isNull();
	}
	
	@Test
	void testDontProvideDocumentationOnHoverWhenEndingWithAnd() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("timer:name1?test=test&"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 15));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get()).isNull();
	}
	
	@Test
	void testDontProvideDocumentationOnUnknownComponent() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("unknowncomponent:"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 15));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get()).isNull();
	}

	@Test
	void testProvideParameterDocumentationOnHover() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("file:bla?filter=test"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 26));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(FILE_FILTER_DOCUMENTATION);		
	}
	
	@Test
	void testRangeForParameterOnHover() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("file:bla?filter=test"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 26));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getRange()).isEqualTo(new Range(new Position(0, 20), new Position(0, 26)));		
	}
	
	@Test
	void testProvideParameterDocumentationForUnknownParamOnHover() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("file:bla?test=test"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 26));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(String.format(OptionParamURIInstance.INVALID_URI_OPTION, "test"));		
	}
	
	@Test
	void testProvideSyntaxForPathParameterOnHover() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("kafka:fl"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 19));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(KAFKA_SYNTAX_HOVER);		
	}
	
	@Test
	void testProvideSyntaxForEmptyPathParameterOnHover() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(createXMLSpringRoute("kafka:"));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 17));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(KAFKA_SYNTAX_HOVER);
	}
}
