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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.ComponentModel.EndpointOptionModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.model.ApiOptionMethodsModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.completion.CamelComponentSchemesCompletionsFuture;
import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;

/**
 * For a Camel URI "timer:timerName?delay=10s", it represents "timerName"
 */
public class PathParamURIInstance extends CamelUriElementInstance {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PathParamURIInstance.class);

	private CamelComponentAndPathUriInstance uriInstance;
	private String value;
	private int pathParamIndex;

	public PathParamURIInstance(CamelComponentAndPathUriInstance uriInstance, String value, int startPosition, int endPosition, int pathParamIndex) {
		super(startPosition, endPosition);
		this.uriInstance = uriInstance;
		this.value = value;
		this.pathParamIndex = pathParamIndex;
	}

	public String getValue() {
		return value;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri, TextDocumentItem docItem) {
		if(pathParamIndex == 0) {
			return getCompletionForApiName(camelCatalog, positionInCamelUri, docItem);
		}
		if(pathParamIndex == 1) {
			return getCompletionForApiMethodName(camelCatalog, positionInCamelUri, docItem);
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	private CompletableFuture<List<CompletionItem>> getCompletionForApiMethodName(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri, TextDocumentItem docItem) {
		return camelCatalog.thenApply(catalog -> {
			ComponentModel model = ModelHelper.generateComponentModel(catalog.componentJSonSchema(getComponentName()), true);
			List<ApiOptionModel> apis = model.getApis();
			if (apis != null && !apis.isEmpty()) {
				Optional<ApiOptionModel> optionModel = apis.stream()
						.filter(apiOption -> {
							PathParamURIInstance apiNamePath = uriInstance.getApiNamePath();
							return apiNamePath != null && apiOption.getName().equals(apiNamePath.getValue());
						}).findAny();
				if (optionModel.isPresent()) {
					List<CompletionItem> completionItems = getCompletionForApiMethodName(optionModel.get());
					String start = value.substring(0, positionInCamelUri - getStartPositionInUri());
					return completionItems.stream()
							.filter(FilterPredicateUtils.matchesCompletionFilter(start))
							.collect(Collectors.toList());
				}
				return Collections.emptyList();
			} else {
				return new CamelComponentSchemesCompletionsFuture(uriInstance, uriInstance.getFilter(positionInCamelUri), docItem).apply(catalog);
			}
		});
	}

	private List<CompletionItem> getCompletionForApiMethodName(ApiOptionModel apiOptionModel) {
		List<CompletionItem> completionItems = new ArrayList<>();
		ApiOptionMethodsModel apiOptionsMethodsModel = apiOptionModel.getApiOptionsMethodsModel();
			Map<String, String> aliasesAsMap = apiOptionModel.getKindToAlias();
			if (apiOptionsMethodsModel.getCreator() != null) {
				completionItems.add(createCompletionItem(aliasesAsMap.get(ApiOptionModel.API_METHOD_KIND_CREATOR)));
			}
			if (apiOptionsMethodsModel.getDeleter() != null) {
				completionItems.add(createCompletionItem(aliasesAsMap.get(ApiOptionModel.API_METHOD_KIND_DELETER)));
			}
			if (apiOptionsMethodsModel.getFetcher() != null) {
				completionItems.add(createCompletionItem(aliasesAsMap.get(ApiOptionModel.API_METHOD_KIND_FETCHER)));
			}
			if (apiOptionsMethodsModel.getReader() != null) {
				completionItems.add(createCompletionItem(aliasesAsMap.get(ApiOptionModel.API_METHOD_KIND_READER)));
			}
			if (apiOptionsMethodsModel.getUpdater() != null) {
				completionItems.add(createCompletionItem(aliasesAsMap.get(ApiOptionModel.API_METHOD_KIND_UPDATER)));
			}
		return completionItems;
	}

	private CompletionItem createCompletionItem(String name) {
		CompletionItem completionItem = new CompletionItem(name);
		completionItem.setInsertText(name);
		CompletionResolverUtils.applyTextEditToCompletionItem(this, completionItem);
		return completionItem;
	}

	private CompletableFuture<List<CompletionItem>> getCompletionForApiName(
			CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri, TextDocumentItem docItem) {
		return camelCatalog.thenApply(catalog -> {
			ComponentModel model = ModelHelper.generateComponentModel(catalog.componentJSonSchema(getComponentName()), true);
			String start = value.substring(0, positionInCamelUri - getStartPositionInUri());
			List<ApiOptionModel> apis = model.getApis();
			if(apis !=null && !apis.isEmpty()) {
				return apis.stream()
					.map(apiOption -> {
						String optionName = apiOption.getName();
						CompletionItem completionItem = new CompletionItem(optionName);
						completionItem.setInsertText(optionName);
						CompletionResolverUtils.applyTextEditToCompletionItem(this, completionItem);
						return completionItem;
					})
					.filter(FilterPredicateUtils.matchesCompletionFilter(start))
					.collect(Collectors.toList());
			} else {
				return new CamelComponentSchemesCompletionsFuture(uriInstance, uriInstance.getFilter(positionInCamelUri), docItem).apply(catalog);
			}
		});
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PathParamURIInstance) {
			return value.equals(((PathParamURIInstance) obj).getValue())
					&& getStartPositionInUri() == ((PathParamURIInstance) obj).getStartPositionInUri()
					&& getEndPositionInUri() == ((PathParamURIInstance) obj).getEndPositionInUri();
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value, getStartPositionInUri(), getEndPositionInUri());
	}
	
	@Override
	public String toString() {
		return "Value: "+value+" start position:" + getStartPositionInUri() + " end position:" + getEndPositionInUri();
	}
	
	@Override
	public String getComponentName() {
		return uriInstance.getComponentName();
	}
	
	@Override
	public String getDescription(ComponentModel componentModel) {
		return componentModel.getSyntax();
	}
	
	@Override
	public CamelURIInstance getCamelUriInstance() {
		return uriInstance.getCamelUriInstance();
	}
	
	public String getName(CompletableFuture<CamelCatalog> camelCatalog) {
		try {
			return camelCatalog.thenApply(catalog -> {
				org.apache.camel.tooling.model.ComponentModel componentModel = catalog.componentModel(uriInstance.getComponentName());
				if (componentModel != null) {
					// here, it is expected that the list is sorted with the correct order of endpoint path in which they appear in the scheme
					List<EndpointOptionModel> endpointPathOptions = componentModel.getEndpointPathOptions();
					if (endpointPathOptions != null && endpointPathOptions.size() >= pathParamIndex) {
						return endpointPathOptions.get(pathParamIndex).getName();
					}
				}
				return null;
			}).get();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			return null;
		} catch(ExecutionException ee) {
			LOGGER.warn("Cannot retrieve Path parameter name", ee);
			return null;
		}
	}
}
