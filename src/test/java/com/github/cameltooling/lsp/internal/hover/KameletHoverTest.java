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

class KameletHoverTest extends AbstractCamelLanguageServerTest {

	@Test
	void testProvideDocumentationOnHover() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"kamelet:aws-ddb-streams-source\" xmlns=\"http://camel.apache.org/schema/spring\"></from>\n");
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 26));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo("Receive events from AWS DynamoDB Streams.");
	}
	
	@Test
	void testProvideGenericDocumentationOnHoverOnUnknownKamelet() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"kamelet:unknown\" xmlns=\"http://camel.apache.org/schema/spring\"></from>\n");
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 19));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo("kamelet:templateId/routeId");
	}
	
	@Test
	void testProvideDocumentationOnHoverOnKnownKameletPropertyName() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"kamelet:aws-ddb-streams-source?secretKey=\" xmlns=\"http://camel.apache.org/schema/spring\"></from>\n");
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 50));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo("The secret key obtained from AWS");
	}
	
}
