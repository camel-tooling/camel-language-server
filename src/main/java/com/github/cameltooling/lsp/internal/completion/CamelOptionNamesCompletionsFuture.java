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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.catalog.model.ApiOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiPropertyMethodOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiPropertyOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.BaseOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.model.EndpointOptionModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.CamelUriElementInstance;
import com.github.cameltooling.lsp.internal.instancemodel.OptionParamKeyURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.OptionParamURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.PathParamURIInstance;

public class CamelOptionNamesCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>>  {

	private CamelUriElementInstance uriElement;
	private String camelComponentName;
	private boolean isProducer;
	private String filterString;
	private int positionInCamelURI;
	private Set<OptionParamURIInstance> alreadyDefinedOptions;

	public CamelOptionNamesCompletionsFuture(CamelUriElementInstance uriElement, String camelComponentName, boolean isProducer, String filterText, int positionInCamelURI, Set<OptionParamURIInstance> alreadyDefinedOptions) {
		this.uriElement = uriElement;
		this.camelComponentName = camelComponentName;
		this.isProducer = isProducer;
		this.filterString = filterText;
		this.positionInCamelURI = positionInCamelURI;
		this.alreadyDefinedOptions = alreadyDefinedOptions;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog catalog) {
		List<BaseOptionModel> allOptions = retrieveAllProperties(catalog);
		return allOptions.stream()
				.filter(endpoint -> "parameter".equals(endpoint.getKind()))
				// filter wrong option groups
				.filter(FilterPredicateUtils.matchesProducerConsumerGroups(isProducer))
				.map(parameter -> {
					CompletionItem completionItem = new CompletionItem(parameter.getName());
					String insertText = parameter.getName();
					
					boolean hasValue = false;
					if (uriElement instanceof OptionParamKeyURIInstance) {
						OptionParamKeyURIInstance param = (OptionParamKeyURIInstance)uriElement;
						hasValue = param.getOptionParamURIInstance().getValue() != null;
					}
					
					if(!hasValue && parameter.getDefaultValue() != null) {
						insertText += String.format("=%s", parameter.getDefaultValue());
					}
					completionItem.setInsertText(insertText);
					completionItem.setDocumentation(parameter.getDescription());
					completionItem.setDetail(parameter.getJavaType());
					completionItem.setDeprecated(parameter.isDeprecated());
					CompletionResolverUtils.applyTextEditToCompletionItem(uriElement, completionItem);
					return completionItem;
				})
				// filter duplicated uri options
				.filter(FilterPredicateUtils.removeDuplicatedOptions(alreadyDefinedOptions, positionInCamelURI))
				.filter(FilterPredicateUtils.matchesCompletionFilter(filterString))
				.collect(Collectors.toList());
	}

	private List<BaseOptionModel> retrieveAllProperties(CamelCatalog catalog) {
		ComponentModel componentModel = ModelHelper.generateComponentModel(catalog.componentJSonSchema(camelComponentName), true);
		List<BaseOptionModel> allOptions = new ArrayList<>();
		allOptions.addAll(componentModel.getEndpointOptions());
		allOptions.addAll(findAvailableApiProperties(componentModel));
		return allOptions;
	}

	private List<EndpointOptionModel> findAvailableApiProperties(ComponentModel componentModel) {
		CamelURIInstance camelUriInstance = uriElement.getCamelUriInstance();
		PathParamURIInstance apiNamePath = camelUriInstance.getComponentAndPathUriElementInstance().getApiNamePath();
		PathParamURIInstance methodNamePath = camelUriInstance.getComponentAndPathUriElementInstance().getMethodNamePath();
		Optional<ApiPropertyMethodOptionModel> apisPropertiesModel = componentModel.getApiProperties()
				.stream()
				.filter(apiProperty -> isCorrespondingApiName(apiNamePath, apiProperty))
				.map(apiProperty -> findApiPropertyModel(componentModel, methodNamePath, apiProperty))
				.filter(Objects::nonNull)
				.findAny();
		if(apisPropertiesModel.isPresent()) {
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
		if(correspondingApi.isPresent()) {
			Map<String, String> aliasesMapping = new HashMap<>();
			for(String aliasFullString : correspondingApi.get().getAliases()) {
				String[] splittedAlias = aliasFullString.split("=");
				aliasesMapping.put(splittedAlias[1], splittedAlias[0]);
			}
			String methodKind = aliasesMapping.get(methodNamePath.getValue());
			if("^creator$".equals(methodKind)) {
				return apiProperty.getCreator();
			} else if("^deleter$".equals(methodKind)) {
				return apiProperty.getDeleter();
			} else if("^fetcher$".equals(methodKind)) {
				return apiProperty.getFetcher();
			} else if("^reader$".equals(methodKind)) {
				return apiProperty.getReader();
			} else if("^updater$".equals(methodKind)) {
				return apiProperty.getUpdater();
			}
		}
		return null;
	}

}
