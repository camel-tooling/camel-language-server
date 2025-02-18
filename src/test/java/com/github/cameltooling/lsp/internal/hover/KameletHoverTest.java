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
import java.util.stream.Stream;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

class KameletHoverTest extends AbstractCamelLanguageServerTest {

	@ParameterizedTest(name = "{0}")
	@MethodSource
	void testHoverDocumentation(String testName, String camelUri, int characterPositionInCamelUriToTest, String expectedHoverContent)
			throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(RouteTextBuilder.createXMLSpringRoute(camelUri));
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + characterPositionInCamelUriToTest));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo(expectedHoverContent);
	}
		
	static Stream<Arguments> testHoverDocumentation() {
	    return Stream.of(
	      Arguments.of("Provide Kamelet documentation on kamelet template id",
	    		  "kamelet:aws-ddb-streams-source", 15, "Receive events from Amazon DynamoDB Streams."),
	      Arguments.of("Provide generic documentation on hover on unknown kamelet",
	    		  "kamelet:unknown", 8, "kamelet:templateId/routeId"),
	      Arguments.of("Provide documentation on hover on known kamelet property name",
	    		  "kamelet:aws-ddb-streams-source?secretKey=", 39, "The secret key obtained from AWS.")
	    );
	}
}
