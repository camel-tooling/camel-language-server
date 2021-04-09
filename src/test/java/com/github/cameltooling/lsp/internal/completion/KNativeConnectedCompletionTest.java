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

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.kubernetes.KnativeConfigManager;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

import io.fabric8.knative.client.KnativeClient;
import io.fabric8.knative.eventing.v1beta1.EventTypeBuilder;
import io.fabric8.knative.messaging.v1.ChannelBuilder;
import io.fabric8.knative.messaging.v1.InMemoryChannelBuilder;
import io.fabric8.knative.mock.EnableKnativeMockClient;
import io.fabric8.knative.serving.v1.ServiceBuilder;

@EnableKnativeMockClient(crud = true)
class KNativeConnectedCompletionTest extends AbstractCamelLanguageServerTest {
	
	private KnativeClient client;
	
	@BeforeEach
	void setup() {
		KnativeConfigManager.getInstance().setClient(client);
	}
	
	@AfterEach
	void teardown() {
		KnativeConfigManager.getInstance().setClient(null);
	}

	@Test
	void testCompletionForChannel() throws Exception {
		String channelName = "myChannel";
		client.channels().create(new ChannelBuilder().withNewMetadata().withName(channelName).endMetadata().build());
		String camelUri = "knative:channel/";
		List<CompletionItem> completions = retrieveCompletions(camelUri);
		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getLabel()).isEqualTo(channelName);
	}
	
	@Test
	void testCompletionForInMemoryChannel() throws Exception {
		String channelName = "myInMemoryChannel";
		client.inMemoryChannels().create(new InMemoryChannelBuilder().withNewMetadata().withName(channelName).endMetadata().build());
		String camelUri = "knative:channel/";
		List<CompletionItem> completions = retrieveCompletions(camelUri);
		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getLabel()).isEqualTo(channelName);
	}
	
	@Test
	void testCompletionForEvent() throws Exception {
		String camelUri = "knative:event/";
		String eventTypeName = "myEventType";
		client.eventTypes().create(new EventTypeBuilder().withNewMetadata().withName(eventTypeName).endMetadata().build());
		List<CompletionItem> completions = retrieveCompletions(camelUri);
		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getLabel()).isEqualTo(eventTypeName);
	}
	
	@Test
	void testCompletionForEndpoint() throws Exception {
		String serviceName = "myService";
		client.services().create(new ServiceBuilder().withNewMetadata().withName(serviceName).endMetadata().build());
		String camelUri = "knative:endpoint/";
		List<CompletionItem> completions = retrieveCompletions(camelUri);
		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getLabel()).isEqualTo(serviceName);
	}
	
	private List<CompletionItem> retrieveCompletions(String camelUri)
			throws URISyntaxException, InterruptedException, ExecutionException {
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		Position position = new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length());
		List<CompletionItem> completions = getCompletionFor(languageServer, position).get().getLeft();
		return completions;
	}
}
