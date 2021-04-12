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
package com.github.cameltooling.lsp.internal.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.consol.citrus.kafka.embedded.EmbeddedKafkaServer;
import com.consol.citrus.kafka.embedded.EmbeddedKafkaServerBuilder;
import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class KafkaTopicCompletionTest extends AbstractCamelLanguageServerTest {

	private EmbeddedKafkaServer kafkaServer;

	@AfterEach
	public void after() throws IOException {
		System.clearProperty(KafkaTopicCompletionProvider.CAMEL_LANGUAGE_SERVER_KAFKA_CONNECTION_URL);
		if (kafkaServer != null) {
			kafkaServer.stop();
		}
	}

	@Test
	void testBasicTopicCompletion() throws Exception {
		String topicName = "topicName";
		initKafkaWithTopics(topicName);

		List<CompletionItem> completions = retrieveCompletionForKafkaTopicPosition();

		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getTextEdit().getLeft().getNewText()).isEqualTo(topicName);
	}

	@Test
	@Timeout(2)
	void testInvalidConnectionUrl() throws Exception {
		String connectionURL = "localhost:9091";
		System.setProperty(KafkaTopicCompletionProvider.CAMEL_LANGUAGE_SERVER_KAFKA_CONNECTION_URL, connectionURL);

		List<CompletionItem> completions = retrieveCompletionForKafkaTopicPosition();

		assertThat(completions).isEmpty();
	}

	@Test
	void testNoConnectionUrlSpecified() throws Exception {
		String topicName = "topicName";
		kafkaServer = new EmbeddedKafkaServerBuilder()
				.kafkaServerPort(9092)
				.topics(topicName)
				.build();
		kafkaServer.start();
		List<CompletionItem> completions = retrieveCompletionForKafkaTopicPosition();

		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getTextEdit().getLeft().getNewText()).isEqualTo(topicName);
	}

	@Test
	void testNoTopic() throws Exception {
		initKafkaWithTopics("");
		List<CompletionItem> completions = retrieveCompletionForKafkaTopicPosition();

		assertThat(completions).isEmpty();
	}
	
	@Test
	void testSeveralTopics() throws Exception {
		initKafkaWithTopics("topic1,topic2");
		List<CompletionItem> completions = retrieveCompletionForKafkaTopicPosition();

		assertThat(completions).hasSize(2);
	}

	private void initKafkaWithTopics(String topics) {
		kafkaServer = new EmbeddedKafkaServerBuilder().topics(topics).build();
		kafkaServer.start();
		String connectionURL = "localhost:" + kafkaServer.getKafkaServerPort();
		System.setProperty(KafkaTopicCompletionProvider.CAMEL_LANGUAGE_SERVER_KAFKA_CONNECTION_URL, connectionURL);
	}

	private List<CompletionItem> retrieveCompletionForKafkaTopicPosition() throws URISyntaxException, InterruptedException, ExecutionException {
		String text = "<from uri=\"kafka:\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		return getCompletionFor(languageServer, new Position(0, 17)).get().getLeft();
	}

}
