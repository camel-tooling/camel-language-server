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

import org.apache.camel.tooling.model.MainModel;
import org.apache.logging.log4j.core.LogEvent;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;
import com.github.cameltooling.lsp.internal.util.TestLogAppender;
import com.github.cameltooling.lsp.internal.util.TestLoggerUtil;
import com.google.gson.Gson;


class CamelLanguageServerTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testProvideCompletionForCamelBlueprintNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(RouteTextBuilder.createXMLBlueprintRoute(""));
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 11));
		
		assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(0, 11, 0, 11));
	}

	@Test
	void testProvideCompletionForToCamelBlueprintNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<to uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 9));
		
		assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(0, 9, 0, 9));
	}
	
	@Test
	void testProvideCompletionForCamelSpringNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(RouteTextBuilder.createXMLSpringRoute(""));
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 11));
		
		assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(0, 11, 0, 11));
	}
	
	@Test
	void testProvideCompletionForJava() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(
				"//camel file\n"+
				"from(\"\")\n",
				".java");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 6));
		
		assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(1, 6, 1, 6));
	}
	
	@Test
	void testProvideCompletionForJavaOnRealFile() throws Exception {
		File f = new File("src/test/resources/workspace/camel.java");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".java");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(15, 14));
			assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(15, 14, 15, 27));
		}
	}
	
	@Nested
	class Yaml {
		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModeline() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLike.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 16));
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(12, 16, 12, 22));
			}
		}
		
		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineOnEmptyURIAttributes() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithEmtpyAttributes.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 16));
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(12, 16, 12, 16));
			}
		}
		
		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineOnEmptyURIAttributesAndSingleQuotes() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithEmtpyAttributesAndSingleQuotes.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 16));
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(12, 16, 12, 16));
			}
		}

		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineWithDoubleQuotesInSingleQuotesInsideURI() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithDoubleQuotesInSingleQuotesInsideURI.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 16));
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(12, 16, 12, 33));
			}
		}

		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineOnEmptyURIAttributesWithoutQuotes() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithEmtpyAttributesWithoutQuotes.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 15));
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(12, 15, 12, 15));
			}
		}

		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineWithURIContainingDoubleQuotes() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithDoubleQuotesInsideURI.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 47));
				CompletionItem expectedanyOrderAttributeCompletionItem = new CompletionItem("anyOrder");
				expectedanyOrderAttributeCompletionItem.setDocumentation("Whether the expected messages should arrive in the same order or can be in any order.");
				expectedanyOrderAttributeCompletionItem.setDeprecated(false);
				expectedanyOrderAttributeCompletionItem.setDetail("boolean");
				expectedanyOrderAttributeCompletionItem.setInsertText("anyOrder=false");
				expectedanyOrderAttributeCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(12, 47), new Position(12, 47)), "anyOrder=false")));
				expectedanyOrderAttributeCompletionItem.setKind(CompletionItemKind.Property);
				assertThat(completions.get().getLeft()).contains(expectedanyOrderAttributeCompletionItem);
			}
		}

		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineWithURIContainingSingleQuotes() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithSingleQuotesInsideURI.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 46));
				CompletionItem expectedanyOrderAttributeCompletionItem = new CompletionItem("anyOrder");
				expectedanyOrderAttributeCompletionItem.setDocumentation("Whether the expected messages should arrive in the same order or can be in any order.");
				expectedanyOrderAttributeCompletionItem.setDeprecated(false);
				expectedanyOrderAttributeCompletionItem.setDetail("boolean");
				expectedanyOrderAttributeCompletionItem.setInsertText("anyOrder=false");
				expectedanyOrderAttributeCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(12, 46), new Position(12, 46)), "anyOrder=false")));
				expectedanyOrderAttributeCompletionItem.setKind(CompletionItemKind.Property);
				assertThat(completions.get().getLeft()).contains(expectedanyOrderAttributeCompletionItem);
			}
		}

		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineWithURIContainingSingleQuotesInSingleQuotes() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithSingleQuotesInSingleQuotesInsideURI.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 47));
				CompletionItem expectedanyOrderAttributeCompletionItem = new CompletionItem("anyOrder");
				expectedanyOrderAttributeCompletionItem.setDocumentation("Whether the expected messages should arrive in the same order or can be in any order.");
				expectedanyOrderAttributeCompletionItem.setDeprecated(false);
				expectedanyOrderAttributeCompletionItem.setDetail("boolean");
				expectedanyOrderAttributeCompletionItem.setInsertText("anyOrder=false");
				expectedanyOrderAttributeCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(12, 47), new Position(12, 47)), "anyOrder=false")));
				expectedanyOrderAttributeCompletionItem.setKind(CompletionItemKind.Property);
				assertThat(completions.get().getLeft()).contains(expectedanyOrderAttributeCompletionItem);
			}
		}

		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineWithURIContainingSingleQuotesInPlain() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithSingleQuotesInPlainInsideURI.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 45));
				CompletionItem expectedanyOrderAttributeCompletionItem = new CompletionItem("anyOrder");
				expectedanyOrderAttributeCompletionItem.setDocumentation("Whether the expected messages should arrive in the same order or can be in any order.");
				expectedanyOrderAttributeCompletionItem.setDeprecated(false);
				expectedanyOrderAttributeCompletionItem.setDetail("boolean");
				expectedanyOrderAttributeCompletionItem.setInsertText("anyOrder=false");
				expectedanyOrderAttributeCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(12, 45), new Position(12, 45)), "anyOrder=false")));
				expectedanyOrderAttributeCompletionItem.setKind(CompletionItemKind.Property);
				assertThat(completions.get().getLeft()).contains(expectedanyOrderAttributeCompletionItem);
			}
		}

		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineWithURIContainingDoubleQuotesInPlain() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithDoubleQuotesInPlainInsideURI.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 45));
				CompletionItem expectedanyOrderAttributeCompletionItem = new CompletionItem("anyOrder");
				expectedanyOrderAttributeCompletionItem.setDocumentation("Whether the expected messages should arrive in the same order or can be in any order.");
				expectedanyOrderAttributeCompletionItem.setDeprecated(false);
				expectedanyOrderAttributeCompletionItem.setDetail("boolean");
				expectedanyOrderAttributeCompletionItem.setInsertText("anyOrder=false");
				expectedanyOrderAttributeCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(12, 45), new Position(12, 45)), "anyOrder=false")));
				expectedanyOrderAttributeCompletionItem.setKind(CompletionItemKind.Property);
				assertThat(completions.get().getLeft()).contains(expectedanyOrderAttributeCompletionItem);
			}
		}

		@Test
		void testProvideCompletionForYamlOnRealFileWithCamelKCloseToModelineWithURIContainingDoubleQuotesInPlainUsingMoreSpaces() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeWithDoubleQuotesInPlainInsideURIUsingMoreSpaces.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(12, 50));
				CompletionItem expectedanyOrderAttributeCompletionItem = new CompletionItem("anyOrder");
				expectedanyOrderAttributeCompletionItem.setDocumentation("Whether the expected messages should arrive in the same order or can be in any order.");
				expectedanyOrderAttributeCompletionItem.setDeprecated(false);
				expectedanyOrderAttributeCompletionItem.setDetail("boolean");
				expectedanyOrderAttributeCompletionItem.setInsertText("anyOrder=false");
				expectedanyOrderAttributeCompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(12, 50), new Position(12, 50)), "anyOrder=false")));
				expectedanyOrderAttributeCompletionItem.setKind(CompletionItemKind.Property);
				assertThat(completions.get().getLeft()).contains(expectedanyOrderAttributeCompletionItem);
			}
		}

		@Test
		void testProvideNoCompletionForYamlOnRealFileWithCamelKCloseToModelineForRestURI() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLike.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(5, 10));
				assertThat(completions.get().getLeft()).isEmpty();
			}
		}

		@Test
		void testProvideCompletionForYamlUsingShortCutOnRealFileWithCamelKCloseToModeline() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeAndShortCut.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(11, 13));
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(11, 13, 11, 19));
			}
		}

		@Test
		void testProvideCompletionForYamlUsingFromOnRealFileWithCamelKCloseToModeline() throws Exception {
			File f = new File("src/test/resources/workspace/samplewithModelineLikeUsingFrom.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(4, 14));
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(4, 14, 4, 24));
			}
		}
		
		@Test
		void testProvideCompletionForYamlKNative() throws Exception {
			File f = new File("src/test/resources/workspace/knative.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(9, 13));
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(9, 13, 9, 23));
			}
		}
		
		@Test
		void testProvideCompletionForYamlCRD() throws Exception {
			File f = new File("src/test/resources/workspace/crd-like.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".yaml");
				Position positionAtBeginningOfFromValue = new Position(8, 15);
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, positionAtBeginningOfFromValue);
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(8, 15, 8, 25));
			}
		}
		
		@Test
		void testProvideCompletionForPlainYaml() throws Exception {
			File f = new File("src/test/resources/workspace/plain.camel.yaml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".camel.yaml");
				Position positionAtBeginningOfFromValue = new Position(1, 10);
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, positionAtBeginningOfFromValue);
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(1, 10, 1, 22));
			}
		}
		
		@Test
		void testProvideCompletionForPlainYml() throws Exception {
			File f = new File("src/test/resources/workspace/plain.camel.yml");
			assertThat(f).exists();
			try (FileInputStream fis = new FileInputStream(f)) {
				CamelLanguageServer cls = initializeLanguageServer(fis, ".camel.yml");
				Position positionAtBeginningOfFromValue = new Position(1, 10);
				CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, positionAtBeginningOfFromValue);
				assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(1, 10, 1, 22));
			}
		}
	}

	@Test
	void testProvideCompletionForApplicationProperties() throws Exception {
		File f = new File("src/test/resources/workspace/application.properties");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServerWithFileName(fis, "application.properties");
			CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(cls, new Position(2, 6), "application.properties");
			String mainJsonSchema = cls.getTextDocumentService().getCamelCatalog().get().mainJsonSchema();
			MainModel mainModel = new Gson().fromJson(mainJsonSchema, MainModel.class);
			assertThat(completions.get().getLeft()).hasSize(mainModel.getGroups().size() + 1);
		}
	}

	@Test
	void testProvideCompletionforMultilineXmlFile() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(
				"<camelContext xmlns=\"http://camel.apache.org/schema/spring\">\n" + 
				"<to uri=\"\" ></to>\n" + 
				"</camelContext>");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 9));
		
		assertThat(completions.get().getLeft()).contains(createExpectedTimerCompletionItem(1, 9, 1, 9));
	}

	@Test
	@Disabled("activate when support of multiline is implemented camel-tooling/camel-language-server#34")
	void testProvideCompletionforMultilineURI() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(
				"<camelContext xmlns=\"http://camel.apache.org/schema/spring\">\n" + 
				"<to uri=\"file:myFolder?\n" + 
				"noop=true&amp;\n" + 
				"recursive=false\n" +
				"\"/>\n\"" +
				"</camelContext>");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(1, 23));
		assertThat(completions.get().getLeft()).hasSizeGreaterThan(10);
	}
	
	@Test
	void testDONTProvideCompletionForNotCamelnamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 11));
		
		assertThat(completions.get().getLeft()).isEmpty();
		assertThat(completions.get().getRight()).isNull();
	}
	
	@Test
	void testDONTProvideCompletionWhenNotAfterURIEqualQuote() throws Exception {
		final TestLogAppender appender = new TestLoggerUtil().setupLogAppender(CamelLanguageServerTest.class.getName());
		
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/spring\"></from>\n");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 6));
		
		assertThat(completions.get().getLeft()).isEmpty();
		assertThat(completions.get().getRight()).isNull();
		for (LogEvent loggingEvent : appender.getLog()) {
			if (loggingEvent.getMessage() != null) {
				assertThat((String)loggingEvent.getMessage().getFormattedMessage()).doesNotContain(CamelEndpointCompletionProcessor.ERROR_SEARCHING_FOR_CORRESPONDING_NODE_ELEMENTS);
			}
		}
	}

	@Test
	void testLoadCamelContextFromFile() throws Exception {
		File f = new File("src/test/resources/workspace/cbr-blueprint.xml");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".xml");
			assertThat(cls).isNotNull();
		}
	}
	
	@Test
	void testLoadJavaCamelContextFromFile() throws Exception {
		File f = new File("src/test/resources/workspace/camel.java");
		assertThat(f).exists();
		try (FileInputStream fis = new FileInputStream(f)) {
			CamelLanguageServer cls = initializeLanguageServer(fis, ".java");
			assertThat(cls).isNotNull();
		}
	}
}

