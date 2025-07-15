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
import org.apache.camel.v1.kameletspec.Definition;
import org.apache.camel.v1.kameletspec.definition.Properties;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;

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

import io.fabric8.kubernetes.api.model.AnyType;


public class CamelOptionNamesCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>>  {

	private final boolean markdown;
	private CamelUriElementInstance uriElement;
	private String camelComponentName;
	private boolean isProducer;
	private String filterString;
	private int positionInCamelURI;
	private Set<OptionParamURIInstance> alreadyDefinedOptions;
	private KameletsCatalogManager kameletsCatalogManager;

	public CamelOptionNamesCompletionsFuture(CamelUriElementInstance uriElement, String camelComponentName, boolean isProducer, String filterText, int positionInCamelURI, Set<OptionParamURIInstance> alreadyDefinedOptions, KameletsCatalogManager kameletsCatalogManager, boolean markdown) {
		this.uriElement = uriElement;
		this.camelComponentName = camelComponentName;
		this.isProducer = isProducer;
		this.filterString = filterText;
		this.positionInCamelURI = positionInCamelURI;
		this.alreadyDefinedOptions = alreadyDefinedOptions;
		this.kameletsCatalogManager = kameletsCatalogManager;
		this.markdown = markdown;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog catalog) {
		ComponentModel componentModel = ModelHelper.generateComponentModel(catalog.componentJSonSchema(camelComponentName), true);
		List<EndpointOptionModel> endpointOptions = componentModel.getEndpointOptions();
		Stream<CompletionItem> endpointOptionsFiltered = initialFilter(endpointOptions).map(createCompletionItem(CompletionItemKind.Property));
		
		List<EndpointOptionModel> availableApiProperties = uriElement.findAvailableApiProperties(componentModel);
		Stream<CompletionItem> availableApiPropertiesFiltered = initialFilter(availableApiProperties).map(createCompletionItem(CompletionItemKind.Variable));
		
		
		Stream<CompletionItem> kameletProperties = retrieveKameletProperties();
		Stream<CompletionItem> twitterGeographySearchProperties = retrieveTwitterGeographySearchProperties();
		
		return Stream.concat( Stream.concat(Stream.concat(endpointOptionsFiltered, availableApiPropertiesFiltered), kameletProperties), twitterGeographySearchProperties)
				// filter duplicated uri options
				.filter(FilterPredicateUtils.removeDuplicatedOptions(alreadyDefinedOptions, positionInCamelURI))
				.filter(FilterPredicateUtils.matchesCompletionFilter(filterString))
				.collect(Collectors.toList());
	}
	
	private Stream<CompletionItem> retrieveTwitterGeographySearchProperties() {
		Stream<CompletionItem> twitterGeographySearchProperties = Stream.empty();
		if (ComponentNameConstants.COMPONENT_NAME_TWITTER_SEARCH.equals(camelComponentName)) {
			return Stream.of(createTwitterGeographySearchCompletionItem());
		}
		return twitterGeographySearchProperties;
	}

	private CompletionItem createTwitterGeographySearchCompletionItem() {
		CompletionItem completionItem = new CompletionItem("twitter-search:<keywords>?<geography-search>");
		String documentation = "Perform a Geography Search using the configured metrics.\n\n"
				+ "`?latitude=<value>&longitude=<value>&radius=<value>&distanceMetric=km|mi`\n\n"
				+ "You need to configure all the following options: latitude, longitude, radius, and distanceMetric.";
		completionItem.setDocumentation(new MarkupContent(MarkupKind.MARKDOWN, documentation));
		completionItem.setInsertTextFormat(InsertTextFormat.Snippet);
		completionItem.setKind(CompletionItemKind.Snippet);
		completionItem.setSortText("1");
		String sep  = this.uriElement.getCamelUriInstance().getDslModelHelper().getParametersSeparator();
		completionItem.setInsertText(
			"latitude=${1:latitude}" + sep + 
			"longitude=${2:longitude}" + sep +
			"radius=${3:radius}" + sep +
			"distanceMetric=${4|km,mi|}");
		CompletionResolverUtils.applyTextEditToCompletionItem(this.uriElement, completionItem);
		return completionItem;
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
				Definition kameletDefinition = kameletsCatalogManager.getCatalog().getKameletDefinition(kameletTemplateId.get());
				if(kameletDefinition != null) {
					kameletProperties = kameletDefinition.getProperties().entrySet().stream().map(this::createCompletionItem);
				}
			}
		}
		return kameletProperties;
	}

	private CompletionItem createCompletionItem(Entry<String, Properties> property) {
		String propertyName = property.getKey();
		CompletionItem completionItem = new CompletionItem(propertyName);
		Properties schema = property.getValue();
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

	private String computeInsertText(String propertyName, Properties schema) {
		AnyType defaultValue = schema.get_default();
		String insertText = propertyName + "=";
		if(defaultValue != null && defaultValue.getValue() != null) {
			insertText += defaultValue.getValue().toString();
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
			if (markdown) {
				completionItem.setDocumentation(getMarkupDocumentation(parameter));
			} else {
				completionItem.setDocumentation(getDocumentation(parameter));
			}
			completionItem.setDetail(parameter.getJavaType());
			completionItem.setKind(kind);
			configureSortTextToHaveApiBasedOptionsBefore(kind, completionItem, insertText);
			CompletionResolverUtils.applyDeprecation(completionItem, parameter.isDeprecated());
			CompletionResolverUtils.applyTextEditToCompletionItem(uriElement, completionItem);
			return completionItem;
		};
	}
	
	private MarkupContent getMarkupDocumentation(BaseOptionModel parameter) {
		StringBuilder doc = new StringBuilder();
		addMarkdownIfNotEmpty(doc,"**Group:** ", parameter.getGroup());
		addMarkdownIfNotEmpty(doc,"**Required:** ", String.valueOf(parameter.isRequired()));
		List<String> values = parameter.getEnums();
		if (values != null) {
			List<String> italic = values.stream().map(s -> "*" + s + "*").collect(Collectors.toList());
			String value = String.join(", ", italic);
			addMarkdownIfNotEmpty(doc,"**Possible values:** ", value);
		}
		String defaultValue = String.valueOf(parameter.getDefaultValue());
		if (defaultValue != null && !defaultValue.isEmpty() && !"null".equals(defaultValue)) {
			addMarkdownIfNotEmpty(doc, "**Default value:** ", "*" + defaultValue + "*");
		}
		doc.append("\\\n");
		doc.append(parameter.getDescription());
		return new MarkupContent(MarkupKind.MARKDOWN, doc.toString());
	}

	private String getDocumentation(BaseOptionModel parameter) {
		StringBuilder doc = new StringBuilder();
		addIfNotEmpty(doc,"Group: ", parameter.getGroup());
		addIfNotEmpty(doc,"Required: ", String.valueOf(parameter.isRequired()));
		List<String> values = parameter.getEnums();
		if (values != null) {
			String value = String.join(", ", values);
			addIfNotEmpty(doc,"Possible values: ", value);
		}
		addIfNotEmpty(doc, "Default value: ", String.valueOf(parameter.getDefaultValue()));
		doc.append('\n');
		doc.append(parameter.getDescription());
		return doc.toString();
	}

	private void addIfNotEmpty(StringBuilder description, String key, String value){
		if (value != null && !value.isEmpty() && !"null".equals(value)) {
			description.append(key);
			description.append(value);
			description.append('\n');
		}
	}
	
	private void addMarkdownIfNotEmpty(StringBuilder description, String key, String value){
		if (value != null && !value.isEmpty() && !"null".equals(value)) {
			description.append(key);
			description.append(value);
			description.append("\\\n");
		}
	}

	private void configureSortTextToHaveApiBasedOptionsBefore(CompletionItemKind kind, CompletionItem completionItem, String insertText) {
		if (CompletionItemKind.Variable.equals(kind)) {
			completionItem.setSortText("1-"+insertText);
		}
	}
}
