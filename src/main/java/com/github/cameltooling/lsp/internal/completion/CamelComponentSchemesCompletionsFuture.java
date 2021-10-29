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
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.instancemodel.CamelUriElementInstance;
import com.github.cameltooling.lsp.internal.instancemodel.ReferenceUtils;
import com.github.cameltooling.lsp.internal.parser.ParserXMLFileHelper;

public final class CamelComponentSchemesCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelComponentSchemesCompletionsFuture.class);
	
	private CamelUriElementInstance uriElement;
	private String filterString;
	private TextDocumentItem docItem;
	
	public CamelComponentSchemesCompletionsFuture(CamelUriElementInstance uriElement, String filterText, TextDocumentItem docItem) {
		this.uriElement = uriElement;
		this.filterString = filterText;
		this.docItem = docItem;
	}
	
	@Override
	public List<CompletionItem> apply(CamelCatalog catalog) {
		List<CompletionItem> result = getCompletionForComponents(catalog);
		if (ReferenceUtils.isReferenceComponentKind(uriElement)) {
			result.addAll(addExistingEndpointsOfSameSchemeCompletionItems());
		}
		return result;
	}

	private List<CompletionItem> getCompletionForComponents(CamelCatalog catalog) {
		return catalog.findComponentNames().stream()
			.map(componentName -> ModelHelper.generateComponentModel(catalog.componentJSonSchema(componentName), true))
			.filter(componentModel -> componentModel.getSyntax() != null)
			.map(componentModel -> {
				CompletionItem completionItem = new CompletionItem(componentModel.getSyntax());
				completionItem.setDocumentation(componentModel.getDescription());
				CompletionResolverUtils.applyDeprecation(completionItem, componentModel.getDeprecated());
				CompletionResolverUtils.applyTextEditToCompletionItem(uriElement, completionItem);
				return completionItem;
			})
			.filter(FilterPredicateUtils.matchesCompletionFilter(filterString))
			.collect(Collectors.toList());
	}
	
	private List<CompletionItem> addExistingEndpointsOfSameSchemeCompletionItems() {
		List<CompletionItem> result = new ArrayList<>();
		try {
			List<String> allEndpointIDsWithScheme = CompletionResolverUtils.retrieveEndpointIDsOfScheme(uriElement.getComponentName(), new ParserXMLFileHelper(), docItem);
			for (String s : allEndpointIDsWithScheme) {
				CompletionItem completionItem = new CompletionItem(s);
				CompletionResolverUtils.applyTextEditToCompletionItem(uriElement, completionItem);
				result.add(completionItem);
			}
		} catch (Exception ex) {
			LOGGER.error("Error retrieving existing {} endpoints!", uriElement.getComponentName(), ex);
		}
		return result;
	}
}