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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tools.lsp.internal.model.EndpointOptionModel;
import org.apache.camel.tools.lsp.internal.model.util.ModelHelper;
import org.eclipse.lsp4j.CompletionItem;

public class CamelOptionSchemaCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>>  {

	private String camelComponentName;

	public CamelOptionSchemaCompletionsFuture(String camelComponentName) {
		this.camelComponentName = camelComponentName;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog catalog) {
		Stream<EndpointOptionModel> endpointOptions = ModelHelper.generateComponentModel(catalog.componentJSonSchema(camelComponentName), true).getEndpointOptions().stream();
		return endpointOptions
				.filter(endpoint -> "parameter".equals(endpoint.getKind()))
				.map(parameter -> {
					CompletionItem completionItem = new CompletionItem(parameter.getName());
					String insertText = parameter.getName() + "=";
					if(parameter.getDefaultValue() != null) {
						insertText += parameter.getDefaultValue();
					}
					completionItem.setInsertText(insertText);
					return completionItem;
				})
				.collect(Collectors.toList());
	}

}
