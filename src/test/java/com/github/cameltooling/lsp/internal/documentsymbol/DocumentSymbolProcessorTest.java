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
package com.github.cameltooling.lsp.internal.documentsymbol;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class DocumentSymbolProcessorTest extends AbstractCamelLanguageServerTest {
	
	@TempDir
	File tempDir;
	
	@Test
	void testRoutesProvidedAsDocumentSymbol() throws Exception {
		String textTotest =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <route id=\"a route\">\r\n" + 
				"      <from uri=\"direct:cafe\"/>\r\n" + 
				"      <split>\r\n" + 
				"        <method bean=\"orderSplitter\"/>\r\n" + 
				"        <to uri=\"direct:drink\"/>\r\n" + 
				"      </split>\r\n" + 
				"    </route>\r\n" + 
				"\r\n" + 
				"    <route id=\"another Route\">\r\n" + 
				"      <from uri=\"direct:drink\"/>\r\n" + 
				"      <recipientList>\r\n" + 
				"        <method bean=\"drinkRouter\"/>\r\n" + 
				"      </recipientList>\r\n" + 
				"    </route>\n"
				+ "</camelContext>\n";
		List<Either<SymbolInformation, DocumentSymbol>> documentSymbols = testRetrieveDocumentSymbol(textTotest, 8);
		DocumentSymbol firstRoute = documentSymbols.stream()
			.filter(item -> "a route".equals(item.getRight().getName()))
			.map(item -> item.getRight())
			.findAny()
			.get();
		Position expectedStart = new Position(2, 24/* expecting 4 but seems a bug in Camel*/);
		Position expectedEnd = new Position(8, 12);
		assertThat(firstRoute.getRange()).usingRecursiveComparison().isEqualTo(new Range(expectedStart, expectedEnd));
	}
	
	@Test
	void testEndpointsInXml() throws Exception {
		String textTotest =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" +
				"    <route id=\"a route\">\r\n" +
				"      <from uri=\"direct:cafe\"/>\r\n" +
				"      <to uri=\"direct:drink\"/>\r\n" +
				"    </route>\r\n" +
				"</camelContext>\n";
		List<Either<SymbolInformation, DocumentSymbol>> documentSymbols = testRetrieveDocumentSymbol(textTotest, 4);
		checkSymbolInformation(documentSymbols, new Position(2, 0), new Position(3, 30), "from direct:cafe");
		checkSymbolInformation(documentSymbols, new Position(3, 0), new Position(3, 30), "to direct:drink");
	}

	private void checkSymbolInformation(List<Either<SymbolInformation, DocumentSymbol>> documentSymbols,
			Position expectedStart, Position expectedEnd, String expectedText) {
		DocumentSymbol from = documentSymbols.stream()
			.filter(item -> {
				return expectedText.equals(item.getRight().getName());
			})
			.map(item -> item.getRight())
			.findAny()
			.get();
		assertThat(from.getRange()).usingRecursiveComparison().isEqualTo(new Range(expectedStart, expectedEnd));
	}

	@Test
	void testEmptyCamelContextReturnCamelContextDocumentSymbol() throws Exception {
		String textTotest =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"</camelContext>\n";
		testRetrieveDocumentSymbol(textTotest, 1);
	}
	
	@Test
	void test2CamelContext() throws Exception {
		String textTotest =
				"<beans xmlns=\"http://www.springframework.org/schema/beans\"\n" + 
				"       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" + 
				"       xsi:schemaLocation=\"\n" + 
				"       http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd\n" + 
				"       http://camel.apache.org/schema/spring https://camel.apache.org/schema/spring/camel-spring.xsd\">\n" + 
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <route id=\"a route\">\r\n" + 
				"      <from uri=\"direct:cafe\"/>\r\n" + 
				"      <split>\r\n" + 
				"        <method bean=\"orderSplitter\"/>\r\n" + 
				"        <to uri=\"direct:drink\"/>\r\n" + 
				"      </split>\r\n" + 
				"    </route>\r\n" + 
				"\r\n" + 
				"    <route id=\"another Route\">\r\n" + 
				"      <from uri=\"direct:drink\"/>\r\n" + 
				"      <recipientList>\r\n" + 
				"        <method bean=\"drinkRouter\"/>\r\n" + 
				"      </recipientList>\r\n" + 
				"    </route>\n"
				+ "</camelContext>\n" +
				"<camelContext id=\"camel2\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <route id=\"a route2\">\r\n" + 
				"      <from uri=\"direct:cafe\"/>\r\n" + 
				"      <split>\r\n" + 
				"        <method bean=\"orderSplitter\"/>\r\n" + 
				"        <to uri=\"direct:drink\"/>\r\n" + 
				"      </split>\r\n" + 
				"    </route>\r\n" + 
				"\r\n" + 
				"    <route id=\"another Route2\">\r\n" + 
				"      <from uri=\"direct:drink\"/>\r\n" + 
				"      <recipientList>\r\n" + 
				"        <method bean=\"drinkRouter\"/>\r\n" + 
				"      </recipientList>\r\n" + 
				"    </route>\n"
				+ "</camelContext>\n"
				+ "</beans>";
		testRetrieveDocumentSymbol(textTotest, 16);
	}
	
	@Test
	void testEmptyRoutes() throws Exception {
		String textTotest =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <route id=\"a route\">\r\n" + 
				"    </route>\r\n" + 
				"\r\n" + 
				"    <route id=\"another Route\">\r\n" + 
				"    </route>\n"
				+ "</camelContext>\n";
		testRetrieveDocumentSymbol(textTotest, 3);
	}
	
	@Test
	void testRoutesWithoutId() throws Exception {
		String textTotest =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <route id=\"a route\">\r\n" + 
				"      <from uri=\"direct:cafe\"/>\r\n" + 
				"      <split>\r\n" + 
				"        <method bean=\"orderSplitter\"/>\r\n" + 
				"        <to uri=\"direct:drink\"/>\r\n" + 
				"      </split>\r\n" + 
				"    </route>\r\n" + 
				"\r\n" + 
				"    <route id=\"another Route\">\r\n" + 
				"      <from uri=\"direct:drink\"/>\r\n" + 
				"      <recipientList>\r\n" + 
				"        <method bean=\"drinkRouter\"/>\r\n" + 
				"      </recipientList>\r\n" + 
				"    </route>\n" +
				"    <route>\r\n" + 
				"      <from uri=\"direct:drink\"/>\r\n" + 
				"      <recipientList>\r\n" + 
				"        <method bean=\"drinkRouter\"/>\r\n" + 
				"      </recipientList>\r\n" + 
				"    </route>\n"
				+ "</camelContext>\n";
		testRetrieveDocumentSymbol(textTotest, 11);
	}
	
	@Test
	void testRoutesProvidedAsDocumentSymbolWithNamespaceprefix() throws Exception {
		String textTotest =
				"<camel:camelContext id=\"camel\" xmlns:camel=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <camel:route id=\"a route\">\r\n" + 
				"      <camel:from uri=\"direct:cafe\"/>\r\n" + 
				"      <camel:split>\r\n" + 
				"        <camel:method bean=\"orderSplitter\"/>\r\n" + 
				"        <camel:to uri=\"direct:drink\"/>\r\n" + 
				"      </camel:split>\r\n" + 
				"    </camel:route>\r\n" + 
				"\r\n" + 
				"    <camel:route id=\"another Route\">\r\n" + 
				"      <camel:from uri=\"direct:drink\"/>\r\n" +
				"      <camel:recipientList>\r\n" + 
				"        <camel:method bean=\"drinkRouter\"/>\r\n" + 
				"      </camel:recipientList>\r\n" + 
				"    </camel:route>\n"
				+ "</camel:camelContext>\n";
		testRetrieveDocumentSymbol(textTotest, 8);
	}
	
	@Test
	void testSingleRoute() throws Exception {
		File f = new File("src/test/resources/workspace/camel.java");
		testSingleRoute(f);
	}

	private void testSingleRoute(File f) throws URISyntaxException, InterruptedException, ExecutionException, IOException {
		List<Either<SymbolInformation,DocumentSymbol>> documentSymbols = testRetrieveDocumentSymbol(f, 4);
		DocumentSymbol documentSymbol = documentSymbols.get(0).getRight();
		assertThat(documentSymbol.getName()).isEqualTo("from file:src/data");
		Range range = documentSymbol.getRange();
		assertThat(range.getStart().getLine()).isEqualTo(15);
		assertThat(range.getEnd().getLine()).isEqualTo(20);
		assertThat(range.getEnd().getCharacter()).isEqualTo(55);
	}
	
	@Test
	void testWithSpaceInPath() throws Exception {
		File f = new File("src/test/resources/workspace with space/MyRouteBuilder.java");
		testSingleRoute(f);
	}
	
	@Test
	void testSeveralRoutes() throws Exception {
		File f = new File("src/test/resources/workspace/My3RoutesBuilder.java");
		List<Either<SymbolInformation,DocumentSymbol>> documentSymbols = testRetrieveDocumentSymbol(f, 12);
		
		List<DocumentSymbol> fromSymbolInformations = documentSymbols.stream()
				.filter(either -> "from direct:route2".equals(either.getRight().getName()))
				.map(Either::getRight)
				.collect(Collectors.toList());
		DocumentSymbol secondFrom = fromSymbolInformations.get(0);
		Range range = secondFrom.getRange();
		assertThat(range.getStart().getLine()).isEqualTo(13);
		assertThat(range.getEnd().getLine()).isEqualTo(18);
		assertThat(range.getEnd().getCharacter()).isEqualTo(55);
	}
	
	@Test
	void testPartialRoute() throws Exception {
		File f = new File("src/test/resources/workspace/PartialRoute.java");
		List<Either<SymbolInformation,DocumentSymbol>> documentSymbols = testRetrieveDocumentSymbol(f, 1);
		
		List<DocumentSymbol> fromSymbolInformations = documentSymbols.stream()
				.filter(either -> "from file:src/data".equals(either.getRight().getName()))
				.map(Either::getRight)
				.collect(Collectors.toList());
		DocumentSymbol secondFrom = fromSymbolInformations.get(0);
		Range range = secondFrom.getRange();
		assertThat(range.getStart().getLine()).isEqualTo(3);
		assertThat(range.getEnd().getLine()).isEqualTo(3);
		assertThat(range.getEnd().getCharacter()).isEqualTo(40);
	}
	
	@Test
	void testInterfaceNotThrowingException() throws Exception {
		File f = new File("src/test/resources/workspace/AnInterface.java");
		testRetrieveDocumentSymbol(f, 0);
	}
	
	@Test
	void testInvalidJavaFileNotThrowingException() throws Exception {
		File f = new File("src/test/resources/workspace/AnInvalid.java");
		testRetrieveDocumentSymbol(f, 0);
	}
	
	private List<Either<SymbolInformation, DocumentSymbol>> testRetrieveDocumentSymbol(String textTotest, int expectedSize) throws URISyntaxException, InterruptedException, ExecutionException, IOException {
		File camelFile = new File(tempDir, "DocumentSymbolInformation.xml");
		Files.writeString(camelFile.toPath(), textTotest);
		return testRetrieveDocumentSymbol(camelFile, expectedSize);
	}
	
	private List<Either<SymbolInformation, DocumentSymbol>> testRetrieveDocumentSymbol(File file, int expectedSize) throws URISyntaxException, InterruptedException, ExecutionException, IOException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(file);
		return testRetrieveDocumentSymbol(expectedSize, camelLanguageServer, file.toURI().toString());
	}

	private List<Either<SymbolInformation, DocumentSymbol>> testRetrieveDocumentSymbol(int expectedSize, CamelLanguageServer camelLanguageServer, String uri) throws InterruptedException, ExecutionException {
		CompletableFuture<List<Either<SymbolInformation,DocumentSymbol>>> documentSymbolFor = getDocumentSymbolFor(camelLanguageServer, uri);
		List<Either<SymbolInformation, DocumentSymbol>> symbolsInformation = documentSymbolFor.get();
		assertThat(symbolsInformation).hasSize(expectedSize);
		return symbolsInformation;
	}
}
