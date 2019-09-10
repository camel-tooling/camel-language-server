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
package com.github.cameltooling.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;


public class CamelLanguageServerTest extends AbstractCamelLanguageServerTest {
	
	@Test
	public void testProvideCompletionForCamelBlueprintNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 11));
		
		assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(0, 11, 0, 11));
	}

	@Test
	public void testProvideCompletionForToCamelBlueprintNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<to uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 9));
		
		assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(0, 9, 0, 9));
	}
	
	@Test
	public void testProvideCompletionForCamelSpringNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/spring\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 11));
		
		assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(0, 11, 0, 11));
	}
	
	@Test
	public void testProvideCompletionForJava() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(
				"//camel file\n"+
				"from(\"\")\n",
				".java");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 6));
		
		assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(1, 6, 1, 6));
	}
	
	@Test
	public void testProvideCompletionForJavaOnRealFile() throws Exception {
		File f = new File("src/test/resources/workspace/camel.java");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".java");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(15, 14));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(15, 14, 15, 27));
		}
	}
	
	@Test
	public void testProvideCompletionForGroovyOnRealFileWithCamelKExtension() throws Exception {
		File f = new File("src/test/resources/workspace/sample.camelk.groovy");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".camelk.groovy");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(0, 6));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(0, 6, 0, 25));
		}
	}
	
	@Test
	public void testProvideCompletionForGroovyOnRealFileWithCamelKShebang() throws Exception {
		File f = new File("src/test/resources/workspace/samplewithshebang.groovy");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".groovy");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(2, 6));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(2, 6, 2, 25));
		}
	}
	
	@Test
	public void testProvideCompletionForKotlinOnRealFileWithCamelKExtension() throws Exception {
		File f = new File("src/test/resources/workspace/sample.camelk.kts");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".camelk.kts");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(0, 6));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(0, 6, 0, 18));
		}
	}
	
	@Test
	public void testProvideCompletionForJSOnRealFileWithCamelKExtension() throws Exception {
		File f = new File("src/test/resources/workspace/sample.camelk.js");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".camelk.js");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(0, 6));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(0, 6, 0, 14));
		}
	}
	
	@Test
	public void testProvideCompletionForGroovyOnRealFileWithCamelKCloseToModeline() throws Exception {
		File f = new File("src/test/resources/workspace/samplewithModelineLike.groovy");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".groovy");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(2, 6));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(2, 6, 2, 25));
		}
	}
	
	@Test
	public void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModeline() throws Exception {
		File f = new File("src/test/resources/workspace/samplewithModelineLike.yaml");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 16));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(12, 16, 12, 22));
		}
	}

	@Test
	public void testProvideCompletionForYamlUsingShortCutOnRealFileWithCamelKCloseToModeline() throws Exception {
		File f = new File("src/test/resources/workspace/samplewithModelineLikeAndShortCut.yaml");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(11, 13));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(11, 13, 11, 19));
		}
	}
	
	@Test
	public void testProvideCompletionForCamelKafkaConnectPropertySink() throws Exception {
		File f = new File("src/test/resources/workspace/camelKafkaconnectSink.properties");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".properties");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(7, 15));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(7, 15, 7, 25));
		}
	}
	
	@Test
	public void testProvideCompletionForCamelKafkaConnectPropertySource() throws Exception {
		File f = new File("src/test/resources/workspace/camelKafkaconnectSource.properties");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".properties");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(8, 17));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(8, 17, 8, 32));
		}
	}

	public void testProvideCompletionForkotlinOnRealFileWithCamelKCloseToModeline() throws Exception {
		File f = new File("src/test/resources/workspace/sampleWithModelineLike.kts");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".kts");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(2, 6));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(2, 6, 2, 18));
		}
	}
	
	@Test
	public void testProvideCompletionForApplicationProperties() throws Exception {
		File f = new File("src/test/resources/workspace/application.properties");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServerWithFileName(fis, "application.properties");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(2, 6), "application.properties");
			assertThat(completions.get().getLeft()).hasSize(4);
		}
	}
	
	@Test
	public void testProvideCompletionForJSOnRealFileWithCamelKCloseToModeline() throws Exception {
		File f = new File("src/test/resources/workspace/sampleWithModelineLike.js");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".js");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(2, 6));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(2, 6, 2, 14));
		}
	}
	
	@Test
	public void testProvideCompletionforMultilineXmlFile() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(
				"<camelContext xmlns=\"http://camel.apache.org/schema/spring\">\n" + 
				"<to uri=\"\" ></to>\n" + 
				"</camelContext>");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 9));
		
		assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(1, 9, 1, 9));
	}

	@Test
	@Disabled("activate when support of multiline is implemented camel-tooling/camel-language-server#34")
	public void testProvideCompletionforMultilineURI() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(
				"<camelContext xmlns=\"http://camel.apache.org/schema/spring\">\n" + 
				"<to uri=\"file:myFolder?\n" + 
				"noop=true&amp;\n" + 
				"recursive=false\n" +
				"\"/>\n\"" +
				"</camelContext>");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 23));
		assertThat(completions.get().getLeft().size()).isGreaterThan(10);
	}
	
	@Test
	public void testDONTProvideCompletionForNotCamelnamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 11));
		
		assertThat(completions.get().getLeft()).isEmpty();
		assertThat(completions.get().getRight()).isNull();
	}
	
	@Test
	public void testDONTProvideCompletionWhenNotAfterURIEqualQuote() throws Exception {
		final TestLogAppender appender = new TestLogAppender();
		final Logger logger = Logger.getRootLogger();
		logger.addAppender(appender);
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/spring\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 6));
		
		assertThat(completions.get().getLeft()).isEmpty();
		assertThat(completions.get().getRight()).isNull();
		for (LoggingEvent loggingEvent : appender.getLog()) {
			if (loggingEvent.getMessage() != null) {
				assertThat((String)loggingEvent.getMessage()).doesNotContain(CamelEndpointCompletionProcessor.ERROR_SEARCHING_FOR_CORRESPONDING_NODE_ELEMENTS);
			}
		}
	}
	
	@Test
	public void testLoadCamelContextFromFile() throws Exception {
		File f = new File("src/test/resources/workspace/cbr-blueprint.xml");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".xml");
			assertThat(cls).isNotNull();
		}
	}
	
	@Test
	public void testLoadJavaCamelContextFromFile() throws Exception {
		File f = new File("src/test/resources/workspace/camel.java");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".java");
			assertThat(cls).isNotNull();
		}
	}
}

