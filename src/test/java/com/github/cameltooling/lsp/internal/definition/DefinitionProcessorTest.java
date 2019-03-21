/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

public class DefinitionProcessorTest extends AbstractCamelLanguageServerTest {
	
	private static final String SIMPLE_DEFINITION = "<camelContext id=\"myContext\" \r\n" + 
			"    xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <endpoint id=\"mySuperId\" uri=\"timer:timerName?delay=1000\"/>\r\n" + 
			"    <route id=\"a route\">\r\n" + 
			"      <from uri='direct:drink1'/>\r\n" + 
			"      <to uri=\"ref:mySuperId\"/>\r\n" + 
			"    </route>"+
			"</camelContext>";
	
	private static final String ENDPOINT_WITHOUT_ID = "<camelContext id=\"myContext\" \r\n" + 
			"    xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <endpoint uri=\"timer:timerName?delay=1000\"/>\r\n" + 
			"    <route id=\"a route\">\r\n" + 
			"      <from uri='direct:drink1'/>\r\n" + 
			"      <to uri=\"ref:mySuperId\"/>\r\n" + 
			"    </route>"+
			"</camelContext>";
	
	private static final String ENDPOINT_UNMATCH_ID = "<camelContext id=\"myContext\" \r\n" + 
			"    xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <endpoint id=\"anotherId\" uri=\"timer:timerName?delay=1000\"/>\r\n" + 
			"    <route id=\"a route\">\r\n" + 
			"      <from uri='direct:drink1'/>\r\n" + 
			"      <to uri=\"ref:mySuperId\"/>\r\n" + 
			"    </route>"+
			"</camelContext>";
	
	@Test
	public void testRetrieveDefinitionSimple() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(SIMPLE_DEFINITION);
		CompletableFuture<Either<List<? extends Location>,List<? extends LocationLink>>> definitions = getDefinitionsFor(camelLanguageServer, new Position(5, 22));
		Location location = definitions.get().getLeft().get(0);
		assertThat(location.getRange().getStart().getLine()).isEqualTo(2);
		assertThat(location.getRange().getEnd().getLine()).isEqualTo(2);
	}
	
	@Test
	public void testRetrieveNoDefinitionWhenEndpointContainsNoId() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(ENDPOINT_UNMATCH_ID);
		CompletableFuture<Either<List<? extends Location>,List<? extends LocationLink>>> definitions = getDefinitionsFor(camelLanguageServer, new Position(5, 22));
		assertThat(definitions.get().getLeft()).isNullOrEmpty();
		assertThat(definitions.get().getRight()).isNullOrEmpty();
	}
	
	@Test
	public void testRetrieveNoDefinitionWhenEndpointContainsNotMatchingIds() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(ENDPOINT_WITHOUT_ID);
		CompletableFuture<Either<List<? extends Location>,List<? extends LocationLink>>> definitions = getDefinitionsFor(camelLanguageServer, new Position(5, 22));
		assertThat(definitions.get().getLeft()).isNullOrEmpty();
		assertThat(definitions.get().getRight()).isNullOrEmpty();
	}
	
	@Test
	public void testRetrieveNoDefinitionWhenOnAnotherPlace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(SIMPLE_DEFINITION);
		CompletableFuture<Either<List<? extends Location>,List<? extends LocationLink>>> definitions = getDefinitionsFor(camelLanguageServer, new Position(6, 22));
		assertThat(definitions.get().getLeft()).isNullOrEmpty();
		assertThat(definitions.get().getRight()).isNullOrEmpty();
	}
}
