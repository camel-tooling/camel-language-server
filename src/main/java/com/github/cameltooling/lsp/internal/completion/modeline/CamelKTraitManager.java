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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitDefinition;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitDefinitionProperty;
import com.google.gson.Gson;

public class CamelKTraitManager {
	
	private CamelKTraitManager() {
		
	}
	
	private static List<TraitDefinition> traits;

	public static List<TraitDefinition> getTraits() {
		if(traits == null) {
			InputStream inputStream = CamelKTraitManager.class.getResourceAsStream("/trait-catalog-camel_k-1.7.0.json");
			String text = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
			traits = Arrays.asList(new Gson().fromJson(text, TraitDefinition[].class));
		}
		return traits;
	}
	
	public static List<CompletionItem> getTraitDefinitionNameCompletionItems(String filter, CamelKModelineTraitDefinition camelKModelineTraitDefinition){
		return getTraits().stream()
				.map(traitDefinition -> traitDefinition.createCompletionItem(camelKModelineTraitDefinition))
				.filter(FilterPredicateUtils.matchesCompletionFilter(filter))
				.collect(Collectors.toList());
	}

	public static List<CompletionItem> getTraitPropertyNameCompletionItems(String filter, CamelKModelineTraitDefinitionProperty traitDefinitionProperty) {
		Optional<TraitDefinition> traitDefinition = getTrait(traitDefinitionProperty.getTraitOption().getTraitDefinition().getValueAsString());
		if(traitDefinition.isPresent()) {
			return traitDefinition.get().getProperties().stream()
					.map(traitProperty -> traitProperty.createCompletionItem(traitDefinitionProperty))
					.filter(FilterPredicateUtils.matchesCompletionFilter(filter))
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}

	private static Optional<TraitDefinition> getTrait(String traitDefinitionName) {
		return getTraits().stream().filter(traitdefinition -> traitDefinitionName.equals(traitdefinition.getName())).findFirst();
	}

	public static String getDescription(String traitDefinitionName) {
		Optional<TraitDefinition> traitDefinition = getTrait(traitDefinitionName);
		if(traitDefinition.isPresent()) {
			return traitDefinition.get().getDescription();
		}
		return null;
	}

	public static String getPropertyDescription(String traitDefinitionName, String traitPropertyName) {
		Optional<TraitDefinition> traitDefinition = getTrait(traitDefinitionName);
		if(traitDefinition.isPresent()) {
			Optional<TraitProperty> traitProperty = getTraitProperty(traitDefinition.get(), traitPropertyName);
			if(traitProperty.isPresent()) {
				return traitProperty.get().getDescription();
			}
		}
		return null;
	}

	private static Optional<TraitProperty> getTraitProperty(TraitDefinition traitDefinition, String traitPropertyName) {
		return traitDefinition.getProperties().stream().filter(traitProperty -> traitPropertyName.equals(traitProperty.getName())).findFirst();
	}

}
