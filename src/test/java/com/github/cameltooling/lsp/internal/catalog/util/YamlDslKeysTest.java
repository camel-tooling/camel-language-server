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
package com.github.cameltooling.lsp.internal.catalog.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.camel.catalog.DefaultCamelCatalog;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.catalog.util.YamlDslModel.Node;

/**
 * The keys of the Camel YAML DSL that the Language Server relies on must be the ones of the YAML DSL model of the
 * bundled catalog: a new Camel version changing them fails these tests.
 */
class YamlDslKeysTest {

	private final YamlDslModel model = YamlDslModel.of(new DefaultCamelCatalog(true));

	@Test
	void testKeysOfRoutes() {
		Node from = model.getNode(YamlDslKeys.FROM);
		assertThat(from.getChild(YamlDslKeys.URI)).isNotNull();
		assertThat(from.getChild(YamlDslKeys.PARAMETERS)).isNotNull();
		assertThat(model.getNode(from.getChild(YamlDslKeys.STEPS))).isSameAs(model.getNode(YamlDslKeys.STEPS));
		assertThat(model.getNode(YamlDslKeys.STEPS).isListChildren()).isTrue();
	}

	@Test
	void testKeysOfKameletEip() {
		Node kamelet = model.getNode(YamlDslKeys.KAMELET);
		assertThat(kamelet.getChild(YamlDslKeys.NAME)).isNotNull();
		assertThat(YamlDslModel.hasEndpointParameters(kamelet)).isTrue();
	}

	@Test
	void testKeyOfExpressions() {
		assertThat(model.getLanguages(model.getNode("setBody"))).isNotNull();
	}

	@Test
	void testConsumerEndpoints() {
		assertThat(YamlDslKeys.CONSUMER_ENDPOINTS).allMatch(name -> YamlDslModel.isEndpoint(model.getNode(name)));
	}
}
