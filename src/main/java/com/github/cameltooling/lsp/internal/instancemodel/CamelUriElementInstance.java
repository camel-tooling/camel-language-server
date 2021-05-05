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
package com.github.cameltooling.lsp.internal.instancemodel;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.catalog.model.ApiOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiPropertyMethodOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiPropertyOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.model.EndpointOptionModel;
import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

public abstract class CamelUriElementInstance implements ILineRangeDefineable{

	private int startPositionInUri;
	private int endPositionInUri;
	private TextDocumentItem document;

	protected CamelUriElementInstance(int startPositionInUri, int endPositionInUri) {
		this.startPositionInUri = startPositionInUri;
		this.endPositionInUri = endPositionInUri;
	}

	public int getStartPositionInUri() {
		return startPositionInUri;
	}

	public int getEndPositionInUri() {
		return endPositionInUri;
	}

	public boolean isInRange(int position) {
		return startPositionInUri <= position && position <= endPositionInUri;
	}
	
	public TextDocumentItem getDocument() {
		return document;
	}

	public void setDocument(TextDocumentItem document) {
		this.document = document;
	}

	public int getStartPositionInLine() {
		return getCamelUriInstance().getStartPositionInDocument().getCharacter() + getStartPositionInUri();
	}
	
	public int getEndPositionInLine() {
		return getCamelUriInstance().getStartPositionInDocument().getCharacter() + getEndPositionInUri();
	}
	
	public int getLine() {
		return getCamelUriInstance().getAbsoluteBounds().getStart().getLine();
	}
	
	public abstract CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri, TextDocumentItem docItem, SettingsManager settingsManager, KameletsCatalogManager kameletsCatalogManager);
	
	public abstract String getComponentName();
	
	public abstract String getDescription(ComponentModel componentModel, KameletsCatalogManager kameletCatalogManager);
	
	public abstract CamelURIInstance getCamelUriInstance();

	public List<EndpointOptionModel> findAvailableApiProperties(ComponentModel componentModel) {
		CamelURIInstance camelUriInstance = getCamelUriInstance();
		PathParamURIInstance apiNamePath = camelUriInstance.getComponentAndPathUriElementInstance().getApiNamePath();
		PathParamURIInstance methodNamePath = camelUriInstance.getComponentAndPathUriElementInstance().getMethodNamePath();
		Optional<ApiPropertyMethodOptionModel> apisPropertiesModel = componentModel.getApiProperties().stream()
				.filter(apiProperty -> isCorrespondingApiName(apiNamePath, apiProperty))
				.map(apiProperty -> findApiPropertyModel(componentModel, methodNamePath, apiProperty))
				.filter(Objects::nonNull).findAny();
		if (apisPropertiesModel.isPresent()) {
			return apisPropertiesModel.get().getProperties();
		} else {
			return Collections.emptyList();
		}
	}
	
	private boolean isCorrespondingApiName(PathParamURIInstance apiNamePath, ApiPropertyOptionModel apiProperty) {
		return apiNamePath != null && apiProperty.getName().equals(apiNamePath.getValue());
	}

	private ApiPropertyMethodOptionModel findApiPropertyModel(ComponentModel componentModel, PathParamURIInstance methodNamePath, ApiPropertyOptionModel apiProperty) {
		Optional<ApiOptionModel> correspondingApi = componentModel.getApis()
				.stream()
				.filter(api -> apiProperty.getName().equals(api.getName()))
				.findAny();
		if(correspondingApi.isPresent() && methodNamePath != null) {
			Map<String, String> aliasesMapping = correspondingApi.get().getAliasToKind();
			String methodKind = aliasesMapping.get(methodNamePath.getValue());
			if(ApiOptionModel.API_METHOD_KIND_CREATOR.equals(methodKind)) {
				return apiProperty.getCreator();
			} else if(ApiOptionModel.API_METHOD_KIND_DELETER.equals(methodKind)) {
				return apiProperty.getDeleter();
			} else if(ApiOptionModel.API_METHOD_KIND_FETCHER.equals(methodKind)) {
				return apiProperty.getFetcher();
			} else if(ApiOptionModel.API_METHOD_KIND_READER.equals(methodKind)) {
				return apiProperty.getReader();
			} else if(ApiOptionModel.API_METHOD_KIND_UPDATER.equals(methodKind)) {
				return apiProperty.getUpdater();
			}
		}
		return null;
	}
}
