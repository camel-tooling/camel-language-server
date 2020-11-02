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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.ComponentModel.EndpointOptionModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		return CompletableFuture.completedFuture(Collections.emptyList());
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
