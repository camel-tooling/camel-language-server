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
package com.github.cameltooling.lsp.internal.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.settings.SettingsManager;

/**
 * The catalog of Camel 4.10 provides no YAML DSL model: the steps are the ones that Camel 4.10 knows.
 */
class YamlDslDiagnosticCatalogVersionTest extends AbstractDiagnosticTest {

	@Test
	void testStepsOfTheCatalogVersion() throws Exception {
		// serviceCall was removed after Camel 4.10, switch was added
		testDiagnostic("yaml-dsl-older-version-steps", 2, ".camel.yaml");
		assertThat(lastPublishedDiagnostics.getDiagnostics()).extracting(this::getDiagnosticMessage)
				.containsExactlyInAnyOrder("'serviceCall' is deprecated", "Unknown Camel EIP or step: 'switch'");
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
