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
package com.github.cameltooling.lsp.internal.modelinemodel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InsertTextFormat;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.completion.FilterPredicateUtils;

public class CamelKModelineDependencyOption implements ICamelKModelineOptionValue {

	private String value;
	private int startPosition;

	public CamelKModelineDependencyOption(String value, int startPosition) {
		this.value = value;
		this.startPosition = startPosition;
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return startPosition + (value != null ? value.length() : 0);
	}

	@Override
	public String getValueAsString() {
		return value;
	}

	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(getStartPositionInLine() <= position && position <= getEndPositionInLine()) {
			String filter = value != null ? value.substring(0, position - getStartPositionInLine()) : "";
			return camelCatalog
					.thenApply(retrieveCamelComponentCompletionItems(filter))
					.thenApply(addMvnDependencyCompletionItem(filter));
		}
		return ICamelKModelineOptionValue.super.getCompletions(position, camelCatalog);
	}

	private Function<List<CompletionItem>, List<CompletionItem>> addMvnDependencyCompletionItem(String filter) {
		return completionItems -> {
			if(filter != null || !"".equals(filter)) {
				completionItems.add(createMvnCompletionItem());
			}
			return completionItems;
		};
	}

	private Function<? super CamelCatalog, ? extends List<CompletionItem>> retrieveCamelComponentCompletionItems(String filter) {
		return catalog -> catalog.findComponentNames().stream()
			.map(componentName -> ModelHelper.generateComponentModel(catalog.componentJSonSchema(componentName), true))
			.map(componentModel -> {
				CompletionItem completionItem = new CompletionItem(componentModel.getArtifactId());
				completionItem.setDocumentation(componentModel.getDescription());
				CompletionResolverUtils.applyDeprecation(completionItem, componentModel.getDeprecated());
				CompletionResolverUtils.applyTextEditToCompletionItem(this, completionItem);
				return completionItem;
			})
			.filter(FilterPredicateUtils.matchesCompletionFilter(filter))
			.collect(Collectors.toList());
	}

	private CompletionItem createMvnCompletionItem() {
		CompletionItem completionItem = new CompletionItem("mvn:<groupId>/<artifactId>:<version>");
		completionItem.setSortText("1"); // allows to be before Camel Components in completion list
		completionItem.setInsertTextFormat(InsertTextFormat.Snippet);
		completionItem.setInsertText("mvn:${1:groupId}/${2:artifactId}:${3:version}");
		CompletionResolverUtils.applyTextEditToCompletionItem(this, completionItem);
		return completionItem;
	}
	
	@Override
	public CompletableFuture<Hover> getHover(int characterPosition, CompletableFuture<CamelCatalog> camelCatalog) {
		return camelCatalog.thenApply(catalog -> {
			Optional<ComponentModel> model = findComponentModel(catalog);
			if (model.isPresent()) {
				return createHover(model.get().getDescription());
			} else {
				return null;
			}
		});
	}

	private Optional<ComponentModel> findComponentModel(CamelCatalog catalog) {
		return catalog.findComponentNames().stream().map(
				componentName -> ModelHelper.generateComponentModel(catalog.componentJSonSchema(componentName), true))
				.filter(componentModel -> value.equals(componentModel.getArtifactId())).findAny();
	}

}
