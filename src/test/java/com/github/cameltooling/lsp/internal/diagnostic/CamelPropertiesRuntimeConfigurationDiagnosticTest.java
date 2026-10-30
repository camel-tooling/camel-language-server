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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.CamelRuntimeProvider;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

class CamelPropertiesRuntimeConfigurationDiagnosticTest extends AbstractDiagnosticTest {

	private static final String FILE_UNDER_TEST = "camel-with-spring-boot-main-properties";

	private String runtimeProviderVersion;

	@Test
	void testCamelMainPropertiesOfSpringBootAreValidOnSpringBoot() throws Exception {
		runtimeProviderVersion = "4.23.0";

		testDiagnostic(FILE_UNDER_TEST, 1, ".properties");

		checkRange(lastPublishedDiagnostics.getDiagnostics().get(0).getRange(), 1, 11, 1, 16);
	}

	@Test
	void testCamelMainPropertiesOfSpringBootAreUnknownWithoutTheRuntimeVersion() throws Exception {
		testDiagnostic(FILE_UNDER_TEST, 2, ".properties");
	}

	@Override
	protected Map<Object, Object> getInitializationOptions() {
		Map<Object, Object> camelSettings = new HashMap<>();
		camelSettings.put(SettingsManager.CATALOG_RUNTIME_PROVIDER, CamelRuntimeProvider.SPRINGBOOT.name());
		camelSettings.put(SettingsManager.CATALOG_RUNTIME_PROVIDER_VERSION, runtimeProviderVersion);
		Map<Object, Object> initializationOptions = new HashMap<>();
		initializationOptions.put(SettingsManager.TOP_LEVEL_SETTINGS_ID, camelSettings);
		return initializationOptions;
	}
}
