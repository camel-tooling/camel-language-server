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
package com.github.cameltooling.lsp.internal.instancemodel.propertiesfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.tooling.model.MainModel;
import org.apache.camel.tooling.model.MainModel.MainOptionModel;
import org.apache.camel.util.StringHelper;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;

import com.github.cameltooling.lsp.internal.catalog.util.StringUtils;
import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

/**
 * Represents one key in properties file. For instance, with
 * "camel.main.autoStartup=true", it is used to represents "main.autoStartup"
 * 
 */
public class CamelGroupPropertyKey implements ILineRangeDefineable {

	private String groupConfiguration;
	private CamelPropertyKeyInstance camelPropertyKeyInstance;
	private String groupName;

	public CamelGroupPropertyKey(String groupProperty, CamelPropertyKeyInstance camelPropertyKeyInstance) {
		this.groupConfiguration = groupProperty;
		this.camelPropertyKeyInstance = camelPropertyKeyInstance;
		int secondDotIndex = groupProperty.indexOf('.');
		if (secondDotIndex != -1) {
			groupName = groupProperty.substring(0, secondDotIndex);
		} else {
			groupName = groupProperty;
		}
	}

	@Override
	public int getLine() {
		return camelPropertyKeyInstance.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return camelPropertyKeyInstance.getStartPositionInLine() + CamelPropertyKeyInstance.CAMEL_KEY_PREFIX.length();
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + groupConfiguration.length();
	}

	public boolean isInRange(int positionChar) {
		return getStartPositionInLine() <= positionChar
				&& positionChar <= groupConfiguration.length() + getStartPositionInLine();
	}

	public CompletableFuture<Hover> getHover(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		if (isInGroupAttribute(position)) {
			return camelCatalog.thenApply(catalog -> {
				if (catalog instanceof DefaultCamelCatalog) {
					MainModel mainModel = ((DefaultCamelCatalog) catalog).mainModel();
					String fullName = CamelPropertyKeyInstance.CAMEL_KEY_PREFIX + groupConfiguration;
					Optional<MainOptionModel> mainOptionModel = findFirstOption(mainModel, fullName);
					if (!mainOptionModel.isPresent() && fullName.contains("-")) {
						mainOptionModel = findFirstOption(mainModel,StringUtils.dashToCamelCase(fullName));
					}
					if (mainOptionModel.isPresent()) {
						return createHover(mainOptionModel.get().getDescription());
					}
				}
				return null;
			});
		}
		return CompletableFuture.completedFuture(null);
	}

	private Optional<MainOptionModel> findFirstOption(MainModel mainModel, String fullName) {
		return mainModel.getOptions().stream()
				.filter(option -> option.getName().startsWith(fullName))
				.findFirst();
	}

	private boolean isInGroupAttribute(Position position) {
		return getStartPositionInLine() + groupName.length() <= position.getCharacter();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		if (isInGroupAttribute(position)) {
			boolean shouldUseDashed = shouldUseDashedCase();
			return camelCatalog.thenApply(catalog -> {
				if (catalog instanceof DefaultCamelCatalog) {
					List<CompletionItem> completions = new ArrayList<>();
					completions.addAll(retrieveCamelMainCompletions(shouldUseDashed, catalog));
					return completions;
				} else {
					return Collections.emptyList();
				}
			});
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	private List<CompletionItem> retrieveCamelMainCompletions(boolean shouldUseDashed, CamelCatalog catalog) {
		MainModel mainModel = ((DefaultCamelCatalog) catalog).mainModel();
		String groupPrefix = CamelPropertyKeyInstance.CAMEL_KEY_PREFIX + groupName + ".";
		return mainModel.getOptions().stream().filter(option -> option.getName().startsWith(groupPrefix))
				.map(option -> {
					String realOptionName = option.getName().substring(groupPrefix.length());
					if(shouldUseDashed) {
						realOptionName = StringHelper.camelCaseToDash(realOptionName);
					}
					CompletionItem completionItem = new CompletionItem(realOptionName);
					completionItem.setDocumentation(option.getDescription());
					CompletionResolverUtils.applyDeprecation(completionItem, option.isDeprecated());
					completionItem.setInsertText(realOptionName + "=");
					return completionItem;
				}).collect(Collectors.toList());
	}

	private boolean shouldUseDashedCase() {
		return camelPropertyKeyInstance.shouldUseDashedCase();
	}

}
