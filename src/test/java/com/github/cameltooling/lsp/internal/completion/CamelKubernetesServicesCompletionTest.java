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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
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

	private KubernetesClient client;

	@Test
	void testCompletionForNamespace() throws Exception {
		KubernetesConfigManager.getInstance().setClient(client);
		createNamespace("first_namespace");

		List<CompletionItem> completions = getCompletionForNamespace();
		assertThat(completions).hasSize(1);
		CompletionItem completion = completions.get(0);
		assertThat(completion.getLabel()).isEqualTo("first_namespace");
		assertThat(completion.getTextEdit().getLeft().getRange()).isEqualTo(new Range(new Position(0, 51), new Position(0, 51)));
	}

	@Test
	void testCompletionWithNoNamespace() throws Exception {
		KubernetesConfigManager.getInstance().setClient(client);

		List<CompletionItem> completions = getCompletionForNamespace();
		assertThat(completions).isEmpty();
	}

	@Test
	void testCompletionWithSeveralNamespace() throws Exception {
		KubernetesConfigManager.getInstance().setClient(client);
		createNamespace("first_namespace");
		createNamespace("second_namespace");

		List<CompletionItem> completions = getCompletionForNamespace();
		assertThat(completions).hasSize(2);
	}

    @Test
    void testCompletionForSecret() throws Exception {
        KubernetesConfigManager.getInstance().setClient(client);
        createNamespace("my-secrets-namespace");
        createSecret("mySecrets", List.of("password"));
        List<CompletionItem> completions = getCompletionForSecrets();
        assertThat(completions).hasSize(1);
        CompletionItem completion = completions.get(0);
        assertThat(completion.getLabel()).isEqualTo("{{secret:mySecrets/password}}");
    }

    @Test
    void testSecretCompletionWithNoSecrets() throws Exception {
        KubernetesConfigManager.getInstance().setClient(client);
        createNamespace("my-secrets-namespace");
        List<CompletionItem> completions = getCompletionForSecrets();
        assertThat(completions).isEmpty();
    }

    @Test
    void testSecretCompletionWithSeveralSecrets() throws Exception {
        KubernetesConfigManager.getInstance().setClient(client);
        createNamespace("my-secrets-namespace");
        createSecret("mySecrets", List.of("key","second"));
        createSecret("myRealSecrets", List.of("another"));

        List<CompletionItem> completions = getCompletionForSecrets();
        assertThat(completions).hasSize(3);
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

}
