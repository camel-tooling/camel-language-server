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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCursorTest;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

/**
 * The catalog of Camel 4.10 provides no YAML DSL model: the keys are the ones of the model of the bundled catalog that
 * Camel 4.10 knows, and the ones removed after Camel 4.10.
 */
class CamelYamlDslCompletionCatalogVersionTest extends AbstractCursorTest {

	private static final String CAMEL_YAML = ".camel.yaml";

	@Test
	void testStepsOfTheCatalogVersion() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - §""", CAMEL_YAML))
			.contains("setBody", "serviceCall", "marshal", "unmarshal")
			.doesNotContain("switch", "when", "otherwise");
	}

	@Test
	void testElementsWhichAreNotNamedLikeTheModelsOfTheCatalog() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - marshal:
				          §""", CAMEL_YAML))
			.contains("json", "bindy", "custom");
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - setBody:
				          §""", CAMEL_YAML))
			.contains("expression", "simple", "method", "language")
			.doesNotContain("python3");
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - setHeaders:
				          §""", CAMEL_YAML))
			.contains("headers");
	}

	@Test
	void testOptionsRemovedAfterTheCatalogVersion() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - transform:
				          to§""", CAMEL_YAML))
			.contains("toType");
	}

	@Override
	protected Map<Object, Object> getInitializationOptions() {
		Map<Object, Object> camelInitializationOptions = new HashMap<>();
		camelInitializationOptions.put(SettingsManager.CAMEL_CATALOG_VERSION, "4.10.8");
		Map<Object, Object> initializationOptions = new HashMap<>();
		initializationOptions.put(SettingsManager.TOP_LEVEL_SETTINGS_ID, camelInitializationOptions);
		return initializationOptions;
	}
}
