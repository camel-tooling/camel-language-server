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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.consol.citrus.kafka.embedded.EmbeddedKafkaServer;
import com.consol.citrus.kafka.embedded.EmbeddedKafkaServerBuilder;
import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

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
	@Timeout(3)
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
	
	@Test
	void testWithProvidedSetting() throws Exception {
		String topic = "topicOnSpecificPort";
		int kafkaPort = 9094;
		kafkaServer = new EmbeddedKafkaServerBuilder()
				.kafkaServerPort(kafkaPort)
				.topics(topic)
				.build();
		kafkaServer.start();
		
		CamelLanguageServer languageServer = initLanguageServer();
		
		DidChangeConfigurationParams params = new DidChangeConfigurationParams(createMapSettings("localhost:"+kafkaPort));
		languageServer.getWorkspaceService().didChangeConfiguration(params);
		
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 17)).get().getLeft();

		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getTextEdit().getLeft().getNewText()).isEqualTo(topic);
	}
	
	private Map<Object, Object> createMapSettings(String kafkaConnectionUrl) {
		Map<Object, Object> settings = new HashMap<>();
		settings.put(SettingsManager.KAKFA_CONNECTION_URL, kafkaConnectionUrl);
		HashMap<Object, Object> initializationOptions = new HashMap<>();
		initializationOptions.put(SettingsManager.TOP_LEVEL_SETTINGS_ID, settings);
		return initializationOptions;
	}

	private void initKafkaWithTopics(String topics) {
		kafkaServer = new EmbeddedKafkaServerBuilder().topics(topics).build();
		kafkaServer.start();
		String connectionURL = "localhost:" + kafkaServer.getKafkaServerPort();
		System.setProperty(KafkaTopicCompletionProvider.CAMEL_LANGUAGE_SERVER_KAFKA_CONNECTION_URL, connectionURL);
	}

	private List<CompletionItem> retrieveCompletionForKafkaTopicPosition() throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer languageServer = initLanguageServer();
		return getCompletionFor(languageServer, new Position(0, 17)).get().getLeft();
	}

	private CamelLanguageServer initLanguageServer() throws URISyntaxException, InterruptedException, ExecutionException {
		String text = RouteTextBuilder.createXMLBlueprintRoute("kafka:");
		return initializeLanguageServer(text, ".xml");
	}

}
