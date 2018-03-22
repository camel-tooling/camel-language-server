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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.model.util.ModelHelper;

public final class CamelComponentSchemaCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>> {
	@Override
	public List<CompletionItem> apply(CamelCatalog catalog) {
		return catalog.findComponentNames().stream()
				.map(componentName -> ModelHelper.generateComponentModel(catalog.componentJSonSchema(componentName), true))
				.map(componentModel -> {
					CompletionItem completionItem = new CompletionItem(componentModel.getSyntax());
					completionItem.setDocumentation(componentModel.getDescription());
					return completionItem;
				}).collect(Collectors.toList());
	}
}