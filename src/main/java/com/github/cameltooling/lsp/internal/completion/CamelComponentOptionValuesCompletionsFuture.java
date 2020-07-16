/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentOptionModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelComponentPropertyKey;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyValueInstance;

public class CamelComponentOptionValuesCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>> {
	
	private static final String BOOLEAN_TYPE = "boolean";
	
	private CamelPropertyValueInstance camelPropertyFileValueInstance;

	private String startFilter;

	public CamelComponentOptionValuesCompletionsFuture(CamelPropertyValueInstance camelPropertyFileValueInstance, String startFilter) {
		this.camelPropertyFileValueInstance = camelPropertyFileValueInstance;
		this.startFilter = startFilter;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog camelCatalog) {
		Optional<ComponentOptionModel> endpointModel = retrieveEndpointOptionModel(camelCatalog);
		if(endpointModel.isPresent()) {
			ComponentOptionModel endpointOptionModel = endpointModel.get();
			List<String> enums = endpointOptionModel.getEnums();
			if (enums != null && !enums.isEmpty()) {
				return computeCompletionForEnums(enums);
			} else if(BOOLEAN_TYPE.equals(endpointOptionModel.getType())) {
				CompletionItem trueCompletion = new CompletionItem(Boolean.TRUE.toString());
				CompletionResolverUtils.applyTextEditToCompletionItem(camelPropertyFileValueInstance, trueCompletion);
				CompletionItem falseCompletion = new CompletionItem(Boolean.FALSE.toString());
				CompletionResolverUtils.applyTextEditToCompletionItem(camelPropertyFileValueInstance, falseCompletion);
				Stream<CompletionItem> values = Stream.of(trueCompletion, falseCompletion);
				return values.filter(FilterPredicateUtils.matchesCompletionFilter(startFilter)).collect(Collectors.toList());
			}
		}
		return Collections.emptyList();
	}
	
	private List<CompletionItem> computeCompletionForEnums(List<String> enums) {
		List<CompletionItem> completionItems = new ArrayList<>();
		for(String enumValue : enums) {
			CompletionItem item = new CompletionItem(enumValue);
			CompletionResolverUtils.applyTextEditToCompletionItem(camelPropertyFileValueInstance, item);
			completionItems.add(item);
		}
		return completionItems.stream().filter(FilterPredicateUtils.matchesCompletionFilter(startFilter)).collect(Collectors.toList());
	}

	private Optional<ComponentOptionModel> retrieveEndpointOptionModel(CamelCatalog camelCatalog) {
		CamelComponentPropertyKey camelComponentPropertyFilekey = camelPropertyFileValueInstance.getKey().getCamelComponentPropertyKey();
		if(camelComponentPropertyFilekey != null) {
			String componentId = camelComponentPropertyFilekey.getComponentId();
			String keyName = camelComponentPropertyFilekey.getComponentProperty();
			if (keyName != null) {
			List<ComponentOptionModel> endpointOptions = ModelHelper.generateComponentModel(camelCatalog.componentJSonSchema(componentId), true).getComponentOptions();
				return endpointOptions.stream()
						.filter(endpoint -> keyName.equals(endpoint.getName()))
						.findAny();
			}
		}
		return Optional.empty();
	}

}
