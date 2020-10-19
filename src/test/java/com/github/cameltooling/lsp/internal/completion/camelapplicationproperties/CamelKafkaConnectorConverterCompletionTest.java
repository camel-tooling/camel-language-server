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
package com.github.cameltooling.lsp.internal.completion.camelapplicationproperties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelKafkaConnectorTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelKafkaConnectorConverterCompletionTest extends AbstractCamelKafkaConnectorTest {

	@Test
	void testProvideCompletionForValueConverter() throws Exception {
		String text = "connector.class=org.test.kafkaconnector.TestSinkConnector\n"
					+ "value.converter=";
		CamelLanguageServer languageServer = initializeLanguageServer(text);
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(1, 16)).get().getLeft();
		assertThat(completions).hasSize(1);
	}
	
	@Test
	void testProvideCompletionForPartialItems() throws Exception {
		String text = "connector.class=org.test.kafkaconnector.TestSinkConnector\n"
					+ "value.converter=org.test";
		CamelLanguageServer languageServer = initializeLanguageServer(text);
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(1, 22)).get().getLeft();
		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getTextEdit().getRange()).isEqualTo(new Range(new Position(1, 16), new Position(1, 24)));
	}

	@Test
	void testProvideCompletionForKeyConverter() throws Exception {
		String text = "connector.class=org.test.kafkaconnector.TestSinkConnector\n"
					+ "key.converter=";
		CamelLanguageServer languageServer = initializeLanguageServer(text);
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(1, 14)).get().getLeft();
		assertThat(completions).hasSize(1);
	}
	
	@Test
	void testProvideNoCompletionWhenNoConverter() throws Exception {
		String text = "connector.class=org.test.kafkaconnector.TestSourceConnector\n"
					+ "value.converter=";
		CamelLanguageServer languageServer = initializeLanguageServer(text);
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(1, 16)).get().getLeft();
		assertThat(completions).isEmpty();
	}
	
	@Test
	void testProvideNoCompletionWhenNoConnectorClass() throws Exception {
		String text = "value.converter=";
		CamelLanguageServer languageServer = initializeLanguageServer(text);
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 16)).get().getLeft();
		assertThat(completions).isEmpty();
	}
	
	@Test
	void testProvideNoCompletionWithUnknownConnectorClass() throws Exception {
		String text = "connector.class=unknown\n"
					+ "value.converter=";
		CamelLanguageServer languageServer = initializeLanguageServer(text);
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(1, 16)).get().getLeft();
		assertThat(completions).isEmpty();
	}

}
