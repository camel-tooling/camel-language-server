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
package com.github.cameltooling.lsp.internal.settings;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.InitializeParams;

import com.github.cameltooling.lsp.internal.CamelTextDocumentService;
import org.eclipse.lsp4j.MarkupKind;

public class SettingsManager {

	public static final String CAMEL_CATALOG_VERSION = "Camel catalog version";
	public static final String TOP_LEVEL_SETTINGS_ID = "camel";
	public static final String EXTRA_COMPONENTS = "extra-components";
	public static final String CATALOG_RUNTIME_PROVIDER = "Camel catalog runtime provider";
	public static final String KAKFA_CONNECTION_URL = "Kafka Connection URL";
	
	private CamelTextDocumentService textDocumentService;
	private String kafkaConnectionUrl;
	private boolean mardownSupport;

	public SettingsManager(CamelTextDocumentService textDocumentService) {
		this.textDocumentService = textDocumentService;
	}

	public void apply(InitializeParams params) {
		applySettings(params.getInitializationOptions());
		ClientCapabilities capabilities = params.getCapabilities();
		if (capabilities != null && capabilities.getTextDocument().getCompletion().getCompletionItem().getDocumentationFormat().contains(MarkupKind.MARKDOWN)) {
			this.mardownSupport = true;
		}
	}
	
	public void apply(DidChangeConfigurationParams params) {
		applySettings(params.getSettings());
	}

	private void applySettings(Object settings) {
		Map<?,?> mapSettings = getSettings(settings);
		Map<?, ?> camelSetting = getSetting(mapSettings, TOP_LEVEL_SETTINGS_ID, Map.class);
		String camelCatalogVersion = getSetting(camelSetting, CAMEL_CATALOG_VERSION, String.class);
		List<?> extraComponents = getSetting(camelSetting, EXTRA_COMPONENTS, List.class);
		String camelCatalogRuntimeProvider = getSetting(camelSetting, CATALOG_RUNTIME_PROVIDER, String.class);
		textDocumentService.updateCatalog(camelCatalogVersion, camelCatalogRuntimeProvider, (List<Map<?, ?>>) extraComponents);
		kafkaConnectionUrl = getSetting(camelSetting, KAKFA_CONNECTION_URL, String.class);
	}

	private Map<?, ?> getSettings(Object settings) {
		Map<?, ?> mapSettings = new JSONUtility().toModel(settings, Map.class);
		return mapSettings == null ? Collections.emptyMap() : mapSettings;
	}
	
	private <T> T getSetting(Map<?, ?> settings, String key, Class<T> clazz) {
		if (settings != null) {
			Object bundleObject = settings.get(key);
			if (clazz.isInstance(bundleObject)) {
				return clazz.cast(bundleObject);
			}
		}
		return null;
	}

	public String getKafkaConnectionUrl() {
		return kafkaConnectionUrl;
	}

	public boolean isMardownSupport() {
		return mardownSupport;
	}
}
