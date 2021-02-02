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
package com.github.cameltooling.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.camel.catalog.DefaultRuntimeProvider;
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.karaf.KarafRuntimeProvider;
import org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider;
import org.apache.camel.springboot.catalog.SpringBootRuntimeProvider;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.CamelRuntimeProvider;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

class CamelCatalogRuntimeProviderTest extends AbstractCamelLanguageServerTest {

	private String runtimeProvider;
	
	@Test
	void testNoValueSpecifiedReturnsDefaultRuntimeProvider() throws Exception {
		testRuntimeProviderWithProvidedValue(null, DefaultRuntimeProvider.class);
	}
	
	@Test
	void testDefaultValueSpecifiedReturnsDefaultRuntimeProvider() throws Exception {
		testRuntimeProviderWithProvidedValue(CamelRuntimeProvider.DEFAULT.name(), DefaultRuntimeProvider.class);
	}
	
	@Test
	void testInvalidValueSpecifiedFallsBackToDefaultRuntimeProvider() throws Exception {
		testRuntimeProviderWithProvidedValue("an invalid value", DefaultRuntimeProvider.class);
	}
	
	@Test
	void testSpringBootValueSpecifiedReturnsSpringBootRuntimeProvider() throws Exception {
		testRuntimeProviderWithProvidedValue(CamelRuntimeProvider.SPRINGBOOT.name(), SpringBootRuntimeProvider.class);
	}
	
	@Test
	void testKarafValueSpecifiedReturnsKarafRuntimeProvider() throws Exception {
		testRuntimeProviderWithProvidedValue(CamelRuntimeProvider.KARAF.name(), KarafRuntimeProvider.class);
	}
	
	@Test
	void testQuarkusValueSpecifiedReturnsQuarkusRuntimeProvider() throws Exception {
		testRuntimeProviderWithProvidedValue(CamelRuntimeProvider.QUARKUS.name(), QuarkusRuntimeProvider.class);
	}
	
	private void testRuntimeProviderWithProvidedValue(String settingValue, Class<? extends RuntimeProvider> expectedRuntimeProviderType)
			throws URISyntaxException, InterruptedException, ExecutionException {
		runtimeProvider = settingValue;
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("");
		RuntimeProvider usedRuntimeProvider = camelLanguageServer.getTextDocumentService().getCamelCatalog().get().getRuntimeProvider();
		assertThat(usedRuntimeProvider).isExactlyInstanceOf(expectedRuntimeProviderType);
	}

	@Override
	protected Map<Object, Object> getInitializationOptions() {
		return createMapSettingsWithRuntimeProvider(runtimeProvider);
	}

	private Map<Object, Object> createMapSettingsWithRuntimeProvider(String runtimeProvider) {
		Map<Object, Object> camelIntializationOptions = new HashMap<>();
		camelIntializationOptions.put(SettingsManager.CATALOG_RUNTIME_PROVIDER, runtimeProvider);
		HashMap<Object, Object> initializationOptions = new HashMap<>();
		initializationOptions.put(SettingsManager.TOP_LEVEL_SETTINGS_ID, camelIntializationOptions);
		return initializationOptions;
	}
	
}
