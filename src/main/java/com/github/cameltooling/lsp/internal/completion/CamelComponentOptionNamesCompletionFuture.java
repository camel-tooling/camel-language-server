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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.util.StringHelper;
import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentOptionModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelComponentParameterPropertyInstance;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyValueInstance;

public class CamelComponentOptionNamesCompletionFuture implements Function<CamelCatalog, List<CompletionItem>> {

	private String componentId;
	private CamelPropertyValueInstance camelPropertyValueInstance;
	private String startFilter;
	private CamelComponentParameterPropertyInstance camelComponentParameterPropertyInstance;

	public CamelComponentOptionNamesCompletionFuture(String componentId, CamelComponentParameterPropertyInstance camelComponentParameterPropertyFileInstance, CamelPropertyValueInstance camelPropertyFileValueInstance, String startFilter) {
		this.componentId = componentId;
		this.camelComponentParameterPropertyInstance = camelComponentParameterPropertyFileInstance;
		this.camelPropertyValueInstance = camelPropertyFileValueInstance;
		this.startFilter = startFilter;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog catalog) {
		Stream<ComponentOptionModel> endpointOptions = ModelHelper.generateComponentModel(catalog.componentJSonSchema(componentId), true).getComponentOptions().stream();
		return endpointOptions
				.map(parameter -> {
					String parameterDisplayName = computeDisplayName(parameter, camelComponentParameterPropertyInstance.shouldUseDashedCase());
					CompletionItem completionItem = new CompletionItem(parameterDisplayName);
					completionItem.setDocumentation(parameter.getDescription());
					completionItem.setDetail(parameter.getJavaType());
					completionItem.setDeprecated(parameter.isDeprecated());
					String insertText = parameterDisplayName;
					if (hasValueProvided() && parameter.getDefaultValue() != null) {
						insertText += String.format("=%s", parameter.getDefaultValue());
					}
					completionItem.setInsertText(insertText);
					CompletionResolverUtils.applyTextEditToCompletionItem(camelComponentParameterPropertyInstance, completionItem);
					return completionItem;
				})
				.filter(FilterPredicateUtils.matchesCompletionFilter(startFilter))
				.collect(Collectors.toList());
	}

	private String computeDisplayName(ComponentOptionModel parameter, boolean useDashedCase) {
		String camelCaseName = parameter.getName();
		if(useDashedCase) {
			return StringHelper.camelCaseToDash(camelCaseName);
		} else {
			return camelCaseName;
		}
	}

	private boolean hasValueProvided() {
		return camelPropertyValueInstance == null || camelPropertyValueInstance.getCamelPropertyFileValue() == null;
	}

}
