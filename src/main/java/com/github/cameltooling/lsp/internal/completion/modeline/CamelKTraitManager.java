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
package com.github.cameltooling.lsp.internal.completion.modeline;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitDefinition;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitDefinitionProperty;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitOption;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;

public class CamelKTraitManager {
	
	private CamelKTraitManager() {
		
	}
	
	private static Map<String, JSONSchemaProps> traits;
	private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	public static Map<String, JSONSchemaProps> getTraits() {
		if(traits == null) {
			InputStream inputStreamCRD = CamelKTraitManager.class.getResourceAsStream("/camel.apache.org_integrations-2.0.1.yaml");	
			try {
				CustomResourceDefinition crd = mapper.readValue(inputStreamCRD, CustomResourceDefinition.class);
				JSONSchemaProps traitsSchema = retrieveTraitsDefinitionFromCamelKCRD(crd);
				traits = traitsSchema.getProperties();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				traits = Collections.emptyMap();
			}
		}
		return traits;
	}

	private static JSONSchemaProps retrieveTraitsDefinitionFromCamelKCRD(CustomResourceDefinition crd) {
		return crd.getSpec().getVersions().get(0).getSchema().getOpenAPIV3Schema().getProperties().get("spec").getProperties().get("traits");
	}
	
	public static List<CompletionItem> getTraitDefinitionNameCompletionItems(String filter, CamelKModelineTraitDefinition camelKModelineTraitDefinition){
		return getTraits().entrySet().stream().map(entryTrait -> {
			CompletionItem completionItem = new CompletionItem(entryTrait.getKey());
			completionItem.setDocumentation(entryTrait.getValue().getDescription());
			if(hasAPropertySpecified(camelKModelineTraitDefinition.getTraitOption())) {
				completionItem.setInsertText(entryTrait.getKey());
			} else {
				completionItem.setInsertText(entryTrait.getKey() + ".");
			}
			CompletionResolverUtils.applyTextEditToCompletionItem(camelKModelineTraitDefinition, completionItem);
			return completionItem;
		}).filter(FilterPredicateUtils.matchesCompletionFilter(filter))
				.collect(Collectors.toList());
	}

	private static boolean hasAPropertySpecified(CamelKModelineTraitOption camelKModelineTraitOption) {
		return camelKModelineTraitOption.getTraitProperty() != null;
	}

	public static List<CompletionItem> getTraitPropertyNameCompletionItems(String filter, CamelKModelineTraitDefinitionProperty traitDefinitionProperty) {
		JSONSchemaProps traitDefinition = getTrait(traitDefinitionProperty.getTraitOption().getTraitDefinition().getValueAsString());
		if(traitDefinition != null) {
			return traitDefinition.getProperties().entrySet().stream()
					.map(traitProperty -> {
						CompletionItem completionItem = new CompletionItem(traitProperty.getKey());
						completionItem.setDocumentation(traitProperty.getValue().getDescription());
						CamelKModelineTraitOption traitOption = traitDefinitionProperty.getTraitOption();
						if (hasAValueSpecified(traitOption)) {
							completionItem.setInsertText(traitProperty.getKey());
						} else {
							completionItem.setInsertText(traitProperty.getKey() + "=");
						}
						CompletionResolverUtils.applyTextEditToCompletionItem(traitDefinitionProperty, completionItem);
						return completionItem;
					})
					.filter(FilterPredicateUtils.matchesCompletionFilter(filter))
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	private static boolean hasAValueSpecified(CamelKModelineTraitOption traitOption) {
		return traitOption.getValueAsString().contains("=");
	}

	private static JSONSchemaProps getTrait(String traitDefinitionName) {
		return getTraits().get(traitDefinitionName);
	}

	public static String getDescription(String traitDefinitionName) {
		JSONSchemaProps traitDefinition = getTrait(traitDefinitionName);
		if(traitDefinition != null) {
			return traitDefinition.getDescription();
		}
		return null;
	}

	public static String getPropertyDescription(String traitDefinitionName, String traitPropertyName) {
		JSONSchemaProps traitDefinition = getTrait(traitDefinitionName);
		if(traitDefinition != null) {
			JSONSchemaProps traitProperty = getTraitProperty(traitDefinition, traitPropertyName);
			if(traitProperty != null) {
				return traitProperty.getDescription();
			}
		}
		return null;
	}

	private static JSONSchemaProps getTraitProperty(JSONSchemaProps traitDefinition, String traitPropertyName) {
		return traitDefinition.getProperties().get(traitPropertyName);
	}

}
