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
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.TestLogAppender;

public class DocumentSymbolProcessorTest extends AbstractCamelLanguageServerTest {
	
	@Test
	public void testRoutesProvidedAsDocumentSymbol() throws Exception {
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
		List<Either<SymbolInformation, DocumentSymbol>> documentSymbols = testRetrieveDocumentSymbol(textTotest, 3);
		SymbolInformation firstRoute = documentSymbols.get(0).getLeft();
		assertThat(firstRoute.getName()).isEqualTo("a route");
		Position expectedStart = new Position(2, 24/* expecting 4 but seems a bug in Camel*/);
		Position expectedEnd = new Position(8, 12);
		assertThat(firstRoute.getLocation()).usingRecursiveComparison().isEqualTo(new Location(DUMMY_URI+".xml", new Range(expectedStart, expectedEnd)));
	}

	@Test
	public void testEmptyCamelContextReturnCamelContextDocumentSymbol() throws Exception {
		String textTotest =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"</camelContext>\n";
		testRetrieveDocumentSymbol(textTotest, 1);
	}
	
	@Test
	public void test2CamelContext() throws Exception {
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
		testRetrieveDocumentSymbol(textTotest, 6);
	}
	
	@Test
	public void testEmptyRoutes() throws Exception {
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
	public void testRoutesWithoutId() throws Exception {
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
		testRetrieveDocumentSymbol(textTotest, 4);
	}
	
	@Test
	public void testRoutesProvidedAsDocumentSymbolWithNamespaceprefix() throws Exception {
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
		testRetrieveDocumentSymbol(textTotest, 3);
	}

	private List<Either<SymbolInformation, DocumentSymbol>> testRetrieveDocumentSymbol(String textTotest, int expectedSize) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textTotest);
		CompletableFuture<List<Either<SymbolInformation,DocumentSymbol>>> documentSymbolFor = getDocumentSymbolFor(camelLanguageServer);
		List<Either<SymbolInformation, DocumentSymbol>> symbolsInformation = documentSymbolFor.get();
		assertThat(symbolsInformation).hasSize(expectedSize);
		return symbolsInformation;
	}
	
	@Test
	public void testNoExceptionWithJavaFile() throws Exception {
		final TestLogAppender appender = new TestLogAppender();
		final Logger logger = Logger.getRootLogger();
		logger.addAppender(appender);
		File f = new File("src/test/resources/workspace/camel.java");
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer camelLanguageServer = initializeLanguageServer(fis, ".java");
			CompletableFuture<List<Either<SymbolInformation,DocumentSymbol>>> documentSymbolFor = getDocumentSymbolFor(camelLanguageServer);
			List<Either<SymbolInformation, DocumentSymbol>> symbolsInformation = documentSymbolFor.get();
			assertThat(symbolsInformation).isEmpty();
			for (LoggingEvent loggingEvent : appender.getLog()) {
				if (loggingEvent.getMessage() != null) {
					assertThat((String)loggingEvent.getMessage()).doesNotContain(DocumentSymbolProcessor.CANNOT_DETERMINE_DOCUMENT_SYMBOLS);
				}
			}
		}
	}

}
