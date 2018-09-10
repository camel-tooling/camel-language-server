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
package com.github.cameltooling.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Ignore;
import org.junit.Test;


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
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(38, 14));
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(38, 14, 38, 27));
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
	@Ignore("activate when support of multiline is implemented camel-tooling/camel-language-server#34")
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
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/spring\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 6));
		
		assertThat(completions.get().getLeft()).isEmpty();
		assertThat(completions.get().getRight()).isNull();
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

