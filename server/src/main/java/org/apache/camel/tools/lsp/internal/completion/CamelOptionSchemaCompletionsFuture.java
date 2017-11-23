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
import org.apache.camel.tools.lsp.internal.model.util.StringUtils;
import org.eclipse.lsp4j.CompletionItem;

public class CamelOptionSchemaCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>>  {

	private String camelComponentUri;

	public CamelOptionSchemaCompletionsFuture(String camelComponentUri) {
		this.camelComponentUri = camelComponentUri;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog catalog) {
		String componentName = StringUtils.asComponentName(camelComponentUri);
		Stream<EndpointOptionModel> endpointOptions = ModelHelper.generateComponentModel(catalog.componentJSonSchema(componentName), true).getEndpointOptions().stream();
		return endpointOptions
				.filter(endpoint -> "parameter".equals(endpoint.getKind()))
				.map(EndpointOptionModel::getName)
				.map(CompletionItem::new)
				.collect(Collectors.toList());
	}

}
