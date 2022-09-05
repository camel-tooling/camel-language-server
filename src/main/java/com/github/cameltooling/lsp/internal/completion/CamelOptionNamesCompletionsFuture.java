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

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.cameltooling.lsp.internal.catalog.model.BaseOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.model.EndpointOptionModel;
import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.instancemodel.CamelUriElementInstance;
import com.github.cameltooling.lsp.internal.instancemodel.ComponentNameConstants;
import com.github.cameltooling.lsp.internal.instancemodel.OptionParamKeyURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.OptionParamURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.PathParamURIInstance;

import io.fabric8.camelk.v1alpha1.JSONSchemaProp;
import io.fabric8.camelk.v1alpha1.JSONSchemaProps;

public class CamelOptionNamesCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>>  {

	private CamelUriElementInstance uriElement;
	private String camelComponentName;
	private boolean isProducer;
	private String filterString;
	private int positionInCamelURI;
	private Set<OptionParamURIInstance> alreadyDefinedOptions;
	private KameletsCatalogManager kameletsCatalogManager;

	public CamelOptionNamesCompletionsFuture(CamelUriElementInstance uriElement, String camelComponentName, boolean isProducer, String filterText, int positionInCamelURI, Set<OptionParamURIInstance> alreadyDefinedOptions, KameletsCatalogManager kameletsCatalogManager) {
		this.uriElement = uriElement;
		this.camelComponentName = camelComponentName;
		this.isProducer = isProducer;
		this.filterString = filterText;
		this.positionInCamelURI = positionInCamelURI;
		this.alreadyDefinedOptions = alreadyDefinedOptions;
		this.kameletsCatalogManager = kameletsCatalogManager;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog catalog) {
		ComponentModel componentModel = ModelHelper.generateComponentModel(catalog.componentJSonSchema(camelComponentName), true);
		List<EndpointOptionModel> endpointOptions = componentModel.getEndpointOptions();
		Stream<CompletionItem> endpointOptionsFiltered = initialFilter(endpointOptions).map(createCompletionItem(CompletionItemKind.Property));
		
		List<EndpointOptionModel> availableApiProperties = uriElement.findAvailableApiProperties(componentModel);
		Stream<CompletionItem> availableApiPropertiesFiltered = initialFilter(availableApiProperties).map(createCompletionItem(CompletionItemKind.Variable));
		
		
		Stream<CompletionItem> kameletProperties = retrieveKameletProperties();
		
		return Stream.concat(Stream.concat(endpointOptionsFiltered, availableApiPropertiesFiltered), kameletProperties)
				// filter duplicated uri options
				.filter(FilterPredicateUtils.removeDuplicatedOptions(alreadyDefinedOptions, positionInCamelURI))
				.filter(FilterPredicateUtils.matchesCompletionFilter(filterString))
				.collect(Collectors.toList());
	}

	private Stream<CompletionItem> retrieveKameletProperties() {
		Stream<CompletionItem> kameletProperties = Stream.empty();
		if(ComponentNameConstants.COMPONENT_NAME_KAMELET.equals(camelComponentName)) {
			Optional<String> kameletTemplateId = uriElement.getCamelUriInstance()
					.getComponentAndPathUriElementInstance()
					.getPathParams()
					.stream()
					.filter(pathParam -> pathParam.getPathParamIndex() == 0)
					.map(PathParamURIInstance::getValue)
					.findAny();
			if(kameletTemplateId.isPresent()) {
				JSONSchemaProps kameletDefinition = kameletsCatalogManager.getCatalog().getKameletDefinition(kameletTemplateId.get());
				if(kameletDefinition != null) {
					kameletProperties = kameletDefinition.getProperties().entrySet().stream().map(this::createCompletionItem);
				}
			}
		}
		return kameletProperties;
	}

	private CompletionItem createCompletionItem(Entry<String, JSONSchemaProp> property) {
		String propertyName = property.getKey();
		CompletionItem completionItem = new CompletionItem(propertyName);
		JSONSchemaProp schema = property.getValue();
		String insertText = computeInsertText(propertyName, schema);
		completionItem.setInsertText(insertText);
		completionItem.setDocumentation(schema.getDescription());
		String type = schema.getType();
		if (type != null) {
			completionItem.setDetail(type);
		}
		CompletionResolverUtils.applyTextEditToCompletionItem(uriElement, completionItem);
		return completionItem;
	}

	private String computeInsertText(String propertyName, JSONSchemaProp schema) {
		JsonNode defaultValue = schema.getDefault();
		String insertText = propertyName + "=";
		if(defaultValue != null && defaultValue.isValueNode()) {
			insertText += defaultValue.asText();
		}
		return insertText;
	}

	private Stream<EndpointOptionModel> initialFilter(List<EndpointOptionModel> endpointOptions) {
		return endpointOptions.stream()
				.filter(endpoint -> "parameter".equals(endpoint.getKind()))
				// filter wrong option groups
				.filter(FilterPredicateUtils.matchesProducerConsumerGroups(isProducer));
	}

	private Function<? super BaseOptionModel, ? extends CompletionItem> createCompletionItem(CompletionItemKind kind) {
		return parameter -> {
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
			completionItem.setKind(kind);
			configureSortTextToHaveApiBasedOptionsBefore(kind, completionItem, insertText);
			CompletionResolverUtils.applyDeprecation(completionItem, parameter.isDeprecated());
			CompletionResolverUtils.applyTextEditToCompletionItem(uriElement, completionItem);
			return completionItem;
		};
	}

	private void configureSortTextToHaveApiBasedOptionsBefore(CompletionItemKind kind, CompletionItem completionItem, String insertText) {
		if(CompletionItemKind.Variable.equals(kind)) {
			completionItem.setSortText("1-"+insertText);
		}
	}
}
