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
import java.util.LinkedHashMap;
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
	public static final String CATALOG_RUNTIME_PROVIDER_VERSION = "Camel catalog runtime provider version";
	public static final String KAKFA_CONNECTION_URL = "Kafka Connection URL";
	/** The directories of the local Kamelets; a relative directory is resolved against the directory of the document. */
	public static final String LOCAL_KAMELET_DIRECTORIES = "local-kamelet-directories";
	/**
	 * The local Kamelet directories of each workspace folder, by the URI of the folder: a document of one of these folders reads
	 * the directories of the innermost folder that contains it instead of {@link #LOCAL_KAMELET_DIRECTORIES}.
	 */
	public static final String WORKSPACE_FOLDER_LOCAL_KAMELET_DIRECTORIES = "workspace-folder-local-kamelet-directories";
	
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
		String camelCatalogRuntimeProviderVersion = getSetting(camelSetting, CATALOG_RUNTIME_PROVIDER_VERSION, String.class);
		textDocumentService.updateCatalog(camelCatalogVersion, camelCatalogRuntimeProvider, camelCatalogRuntimeProviderVersion, (List<Map<?, ?>>) extraComponents);
		kafkaConnectionUrl = getSetting(camelSetting, KAKFA_CONNECTION_URL, String.class);
		List<?> localKameletDirectories = getSetting(camelSetting, LOCAL_KAMELET_DIRECTORIES, List.class);
		textDocumentService.getKameletsCatalogManager().setLocalKameletDirectories(localKameletDirectories == null ? null
				: toStrings(localKameletDirectories));
		Map<?, ?> workspaceFolderLocalKameletDirectories = getSetting(camelSetting, WORKSPACE_FOLDER_LOCAL_KAMELET_DIRECTORIES, Map.class);
		textDocumentService.getKameletsCatalogManager().setWorkspaceFolderLocalKameletDirectories(
				toDirectoriesByFolder(workspaceFolderLocalKameletDirectories));
	}

	/**
	 * @return the directories of each folder, without the entries whose folder is not a string or whose directories are not a list
	 */
	private static Map<String, List<String>> toDirectoriesByFolder(Map<?, ?> directoriesByFolder) {
		Map<String, List<String>> result = new LinkedHashMap<>();
		if (directoriesByFolder != null) {
			directoriesByFolder.forEach((folder, directories) -> {
				if (folder instanceof String folderUri && directories instanceof List<?> list) {
					result.put(folderUri, toStrings(list));
				}
			});
		}
		return result;
	}

	private static List<String> toStrings(List<?> values) {
		return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
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
