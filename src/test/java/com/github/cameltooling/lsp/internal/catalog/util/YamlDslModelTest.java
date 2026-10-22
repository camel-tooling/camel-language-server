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

import java.util.List;

import org.apache.camel.catalog.DefaultCamelCatalog;
import org.junit.jupiter.api.Test;

class YamlDslModelTest {

	private final YamlDslModel model = YamlDslModel.of(new DefaultCamelCatalog(true));

	@Test
	void testBundledCatalogProvidesItsModel() {
		assertThat(model.isAvailable()).isTrue();
		assertThat(model.isOwnModel()).isTrue();
	}

	@Test
	void testEndpointsAreTheElementsWithUriAndParameters() {
		assertThat(List.of("from", "to", "toD", "wireTap", "poll", "switchCase", "switchOtherwise"))
			.allMatch(name -> YamlDslModel.isEndpoint(model.getNode(name)));
		// the uri of an intercept can be a pattern
		assertThat(List.of("interceptFrom", "interceptSendToEndpoint", "routeTemplate", "kamelet"))
			.noneMatch(name -> YamlDslModel.isEndpoint(model.getNode(name)));
	}

	@Test
	void testPathsAreResolvedWithTheReferencedNodes() {
		assertThat(model.resolve(List.of("from", "steps", "switch", "case")).getName()).isEqualTo("switchCase");
		assertThat(model.resolve(List.of("route", "from")).getName()).isEqualTo("from");
		assertThat(model.resolve(List.of("from", "steps", "setBody", "simple")).getName()).isEqualTo("simple");
		assertThat(model.resolve(List.of("routeConfiguration", "onException", "onException")).getName()).isEqualTo("onException");
		assertThat(model.resolve(List.of("from", "steps", "unknown"))).isNull();
	}
}
