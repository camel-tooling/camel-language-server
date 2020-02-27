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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.instancemodel.OptionParamValueURIInstance;
import com.github.cameltooling.model.EndpointOptionModel;
import com.github.cameltooling.model.util.ModelHelper;

public class CamelOptionValuesCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>> {

	private static final String BOOLEAN_TYPE = "boolean";
	private OptionParamValueURIInstance optionParamValueURIInstance;
	private String filterString;

	public CamelOptionValuesCompletionsFuture(OptionParamValueURIInstance optionParamValueURIInstance, String filterText) {
		this.optionParamValueURIInstance = optionParamValueURIInstance;
		this.filterString = filterText;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog camelCatalog) {
		Optional<EndpointOptionModel> endpointModel = retrieveEndpointOptionModel(camelCatalog);
		if(endpointModel.isPresent()) {
			EndpointOptionModel endpointOptionModel = endpointModel.get();
			List<String> enums = endpointOptionModel.getEnums();
			if (enums != null && !enums.isEmpty()) {
				return computeCompletionForEnums(enums);
			} else if(BOOLEAN_TYPE.equals(endpointOptionModel.getType())) {
				CompletionItem trueItem = new CompletionItem(Boolean.TRUE.toString());
				CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, trueItem);
				CompletionItem falseItem =  new CompletionItem(Boolean.FALSE.toString());
				CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, falseItem);
				Stream<CompletionItem> values = Stream.of(trueItem, falseItem);
				return values.filter(FilterPredicateUtils.matchesCompletionFilter(filterString)).collect(Collectors.toList());
			}
		}
		return Collections.emptyList();
	}

	private List<CompletionItem> computeCompletionForEnums(List<String> enums) {
		List<CompletionItem> completionItems = new ArrayList<>();
		for(String enumValue : enums) {
			CompletionItem item = new CompletionItem(enumValue);
			CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, item);
			completionItems.add(item);
		}
		return completionItems.stream().filter(FilterPredicateUtils.matchesCompletionFilter(filterString)).collect(Collectors.toList());
	}

	private Optional<EndpointOptionModel> retrieveEndpointOptionModel(CamelCatalog camelCatalog) {
		String componentName = optionParamValueURIInstance.getOptionParamURIInstance().getComponentName();
		String keyName = optionParamValueURIInstance.getOptionParamURIInstance().getKey().getKeyName();
		List<EndpointOptionModel> endpointOptions = ModelHelper.generateComponentModel(camelCatalog.componentJSonSchema(componentName), true).getEndpointOptions();
		return endpointOptions.stream()
				.filter(endpoint -> keyName.equals(endpoint.getName()))
				.findAny();
	}
}
