/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import jakarta.inject.Inject;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.kubernetes.KubernetesConfigManager;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;

@EnableKubernetesMockClient(crud = true)
class CamelKubernetesServicesCompletionTest extends AbstractCamelLanguageServerTest {

	@Inject
	private KubernetesClient client;

	@BeforeEach
	void setupCluster() {
		KubernetesConfigManager.getInstance().setClient(client);
		client.secrets().delete();
		client.namespaces().delete();
		client.configMaps().delete();
	}
	@AfterEach
	void cleanupCluster() {
		KubernetesConfigManager.getInstance().setClient(null);
	}

	@Test
	void testCompletionForNamespace() throws Exception {
		createNamespace("first_namespace");
		List<CompletionItem> completions = getCompletionForNamespace();
		assertThat(completions).hasSize(1);
		CompletionItem completion = completions.get(0);
		assertThat(completion.getLabel()).isEqualTo("first_namespace");
		assertThat(completion.getTextEdit().getLeft().getRange()).isEqualTo(new Range(new Position(0, 51), new Position(0, 51)));
	}

	@Test
	void testCompletionWithNoNamespace() throws Exception {
		List<CompletionItem> completions = getCompletionForNamespace();
		assertThat(completions).isEmpty();
	}

	@Test
	void testCompletionWithSeveralNamespace() throws Exception {
		createNamespace("first_namespace");
		createNamespace("second_namespace");

		List<CompletionItem> completions = getCompletionForNamespace();
		assertThat(completions).hasSize(2);
	}

	@Test
	void testCompletionForSecret() throws Exception {
		createNamespace("my-secrets-namespace");
		createSecret("mySecrets", List.of("password"));
		List<CompletionItem> completions = getCompletionForSecrets();
		assertThat(completions).hasSize(1);
		CompletionItem completion = completions.get(0);
		assertThat(completion.getLabel()).isEqualTo("{{secret:mySecrets/password}}");
	}

	@Test
	void testSecretCompletionWithNoSecrets() throws Exception {
		createNamespace("my-secrets-namespace");
		List<CompletionItem> completions = getCompletionForSecrets();
		assertThat(completions).isEmpty();
	}

	@Test
	void testSecretCompletionWithSeveralSecrets() throws Exception {
		createNamespace("my-secrets-namespace");
		createSecret("mySecrets", List.of("key", "second"));
		createSecret("myRealSecrets", List.of("another"));

		List<CompletionItem> completions = getCompletionForSecrets();
		assertThat(completions).hasSize(3);
	}

	@Test
	void testConfigMapCompletion() throws Exception {
		createNamespace("my-secrets-namespace");
		createConfigMap("myMap", List.of("myKey1", "myKey2"));

		List<CompletionItem> completions = getCompletionForConfigMaps();
		assertThat(completions).hasSize(2);
		assertThat(completions.get(0).getLabel()).isEqualTo("{{configmap:myMap/myKey1}}");
		assertThat(completions.get(1).getLabel()).isEqualTo("{{configmap:myMap/myKey2}}");
	}

	@Test
	void testConfigMapAndSecretsCompletion() throws Exception {
		createNamespace("my-secrets-namespace");
		createSecret("mySecrets", List.of("key", "second"));
		createConfigMap("myMap", List.of("myKey1", "myKey2"));

		List<CompletionItem> completions = getCompletionForPlaceholders();
		assertThat(completions).hasSize(4);
		assertThat(completions.get(0).getLabel()).isEqualTo("{{secret:mySecrets/key}}");
		assertThat(completions.get(1).getLabel()).isEqualTo("{{secret:mySecrets/second}}");
		assertThat(completions.get(2).getLabel()).isEqualTo("{{configmap:myMap/myKey1}}");
		assertThat(completions.get(3).getLabel()).isEqualTo("{{configmap:myMap/myKey2}}");
	}

	@Test
	void testKubernetesAutoCompletionMidValue() throws Exception {
		createNamespace("my-secrets-namespace");
		createConfigMap("myMap", List.of("myKey1", "myKey2"));

		String camelUri = "pgevent:host:999/database/channel?user=something{{a}}afterwards";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		Position position = new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length() - 12);
		List<CompletionItem> completions = getCompletionFor(languageServer, position).get().getLeft();

		assertThat(completions).hasSize(2);
		assertThat(completions.get(0).getLabel()).isEqualTo("{{configmap:myMap/myKey1}}");
		assertThat(completions.get(0).getFilterText()).isEqualTo("something{{configmap:myMap/myKey1}}");
		assertThat(completions.get(1).getLabel()).isEqualTo("{{configmap:myMap/myKey2}}");
		assertThat(completions.get(1).getInsertText()).isEqualTo("something{{configmap:myMap/myKey2}}afterwards");
	}

	@Test
	void testKubernetesAutoCompletionSeveralPlaceholders() throws Exception {
		createNamespace("my-secrets-namespace");
		createConfigMap("myMap", List.of("myKey1", "myKey2"));

		String camelUri = "pgevent:host:999/database/channel?user={{secret:mySec/none}}something{{co}}afterwards" +
				"{{secret:aa/BB}}";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		Position position = new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length() - 30);
		List<CompletionItem> completions = getCompletionFor(languageServer, position).get().getLeft();

		assertThat(completions).hasSize(2);
		assertThat(completions.get(0).getLabel()).isEqualTo("{{configmap:myMap/myKey1}}");
		assertThat(completions.get(0).getFilterText()).isEqualTo("{{secret:mySec/none}}something{{configmap:myMap/myKey1}}");
		assertThat(completions.get(1).getLabel()).isEqualTo("{{configmap:myMap/myKey2}}");
		assertThat(completions.get(1).getInsertText()).isEqualTo("{{secret:mySec/none}}something{{configmap:myMap/myKey2}}afterwards{{secret:aa/BB}}");
	}

	@Test
	void testKubernetesAutoCompletionPlaceholdersNotClosed() throws Exception {
		createNamespace("my-secrets-namespace");
		createConfigMap("myMap", List.of("myKey1", "myKey2"));

		String camelUri = "pgevent:host:999/database/channel?user=}}something{{a";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		Position position = new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length());
		List<CompletionItem> completions = getCompletionFor(languageServer, position).get().getLeft();

		assertThat(completions).hasSize(2);
		assertThat(completions.get(0).getLabel()).isEqualTo("{{configmap:myMap/myKey1}}");
		assertThat(completions.get(0).getFilterText()).isEqualTo("}}something{{configmap:myMap/myKey1}}");
		assertThat(completions.get(1).getLabel()).isEqualTo("{{configmap:myMap/myKey2}}");
		assertThat(completions.get(1).getInsertText()).isEqualTo("}}something{{configmap:myMap/myKey2}}");
	}

	private void createNamespace(String name) {
		client.namespaces().resource(new NamespaceBuilder().withNewMetadata().withName(name).endMetadata().build()).create();
	}

	private List<CompletionItem> getCompletionForNamespace()
			throws URISyntaxException, InterruptedException, ExecutionException {
		String camelUri = "kubernetes-services:masterUrl?namespace=";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		Position position = new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length());
		List<CompletionItem> completions = getCompletionFor(languageServer, position).get().getLeft();
		return completions;
	}

	private void createSecret(String secretName, List<String> keys) {
		Map<String, String> data = new HashMap<>();
		keys.forEach(k -> data.put(k, k + "encrypted"));
		client.secrets().resource(
				new SecretBuilder()
						.withNewMetadata()
						.withName(secretName)
						.withNamespace("my-secrets-namespace")
						.withLabels(Map.of())
						.endMetadata()
						.withData(data)
						.build())
				.create();
	}

	private List<CompletionItem> getCompletionForSecrets()
			throws URISyntaxException, InterruptedException, ExecutionException {
		String camelUri = "pgevent:host:999/database/channel?user={{secret:";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		Position position = new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length());
		List<CompletionItem> completions = getCompletionFor(languageServer, position).get().getLeft();
		return completions;
	}

	private void createConfigMap(String configMapName, List<String> keys) {
		Map<String, String> data = new HashMap<>();
		keys.forEach(k -> data.put(k, k + "encrypted"));
		client.configMaps().resource(
						new ConfigMapBuilder()
								.withNewMetadata()
								.withName(configMapName)
								.withNamespace("my-secrets-namespace")
								.withLabels(Map.of())
								.endMetadata()
								.withData(data)
								.build())
				.create();
	}


	private List<CompletionItem> getCompletionForConfigMaps()
			throws URISyntaxException, InterruptedException, ExecutionException {
		String camelUri = "pgevent:host:999/database/channel?user={{configmap:";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		Position position = new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length());
		List<CompletionItem> completions = getCompletionFor(languageServer, position).get().getLeft();
		return completions;
	}


	private List<CompletionItem> getCompletionForPlaceholders()
			throws URISyntaxException, InterruptedException, ExecutionException {
		String camelUri = "pgevent:host:999/database/channel?user={{";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		Position position = new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length());
		List<CompletionItem> completions = getCompletionFor(languageServer, position).get().getLeft();
		return completions;
	}
}
