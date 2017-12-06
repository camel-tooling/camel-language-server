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
package org.apache.camel.tools.lsp.internal.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tools.lsp.internal.model.EndpointOptionModel;
import org.apache.camel.tools.lsp.internal.model.util.ModelHelper;
import org.apache.camel.tools.lsp.internal.parser.OptionParamValueURIInstance;
import org.eclipse.lsp4j.CompletionItem;

public class CamelOptionEnumValuesCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>> {

	private OptionParamValueURIInstance optionParamValueURIInstance;

	public CamelOptionEnumValuesCompletionsFuture(OptionParamValueURIInstance optionParamValueURIInstance) {
		this.optionParamValueURIInstance = optionParamValueURIInstance;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog camelCatalog) {
		String componentName = optionParamValueURIInstance.getOptionParamURIInstance().getComponentName();
		String keyName = optionParamValueURIInstance.getOptionParamURIInstance().getKey().getKeyName();
		List<EndpointOptionModel> endpointOptions = ModelHelper.generateComponentModel(camelCatalog.componentJSonSchema(componentName), true).getEndpointOptions();
		Optional<EndpointOptionModel> endpointModel = endpointOptions.stream()
				.filter(endpoint -> keyName.equals(endpoint.getName()))
				.findAny();
		if(endpointModel.isPresent()) {
			String enums = endpointModel.get().getEnums();
			if(enums != null) {
				List<CompletionItem> completionItems = new ArrayList<>();
				for(String enumValue : enums.split(",")) {
					completionItems.add(new CompletionItem(enumValue));
				}
				return completionItems;
			}
		}
		return Collections.emptyList();
	}

}
