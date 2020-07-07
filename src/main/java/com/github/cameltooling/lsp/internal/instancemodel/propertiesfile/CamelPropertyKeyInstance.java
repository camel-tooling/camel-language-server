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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.tooling.model.MainModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;

import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;
import com.google.gson.Gson;

/**
 * Represents one key in properties file.
 * For instance, with "camel.component.timer.delay=1000",
 * it is used to represents "camel.component.timer.delay"
 * 
 */
public class CamelPropertyKeyInstance implements ILineRangeDefineable {
	
	private static final String CAMEL_KEY_PREFIX = "camel.";
	static final String CAMEL_COMPONENT_KEY_PREFIX = "camel.component.";
	
	private String camelPropertyKey;
	private String propertyGroup;
	private CamelComponentPropertyKey camelComponentPropertyKey;
	private CamelPropertyEntryInstance camelPropertyEntryInstance;

	public CamelPropertyKeyInstance(String camelPropertyFileKey, CamelPropertyEntryInstance camelPropertyEntryInstance) {
		this.camelPropertyKey = camelPropertyFileKey;
		this.camelPropertyEntryInstance = camelPropertyEntryInstance;
		if (camelPropertyFileKey.startsWith(CAMEL_COMPONENT_KEY_PREFIX)) {
			camelComponentPropertyKey = new CamelComponentPropertyKey(camelPropertyFileKey.substring(CAMEL_COMPONENT_KEY_PREFIX.length()), this);
		}
		if(camelPropertyKey.startsWith(CAMEL_KEY_PREFIX)) {
			int startIndexGroup = CAMEL_KEY_PREFIX.length();
			int secondDotIndex = camelPropertyKey.indexOf('.', startIndexGroup);
			if (secondDotIndex != -1) {
				propertyGroup = camelPropertyFileKey.substring(startIndexGroup, secondDotIndex);
			} else {
				propertyGroup = camelPropertyFileKey.substring(startIndexGroup);
			}
		}
	}

	public int getEndposition() {
		return camelPropertyEntryInstance.getStartPositionInLine() + camelPropertyKey.length();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(position.getCharacter() == getStartPositionInLine()) {
			return CompletableFuture.completedFuture(Collections.singletonList(new CompletionItem(CAMEL_KEY_PREFIX)));
		} else if (getStartPositionInLine() + CAMEL_KEY_PREFIX.length() == position.getCharacter() && camelPropertyKey.startsWith(CAMEL_KEY_PREFIX)) {
			return getTopLevelCamelCompletion(camelCatalog);
		} else if(camelComponentPropertyKey != null && camelComponentPropertyKey.isInRange(position.getCharacter())) {
			return camelComponentPropertyKey.getCompletions(position, camelCatalog);
		} else if(propertyGroup != null && getStartPositionInLine() + CAMEL_KEY_PREFIX.length() + propertyGroup.length() + 1 == position.getCharacter()) {
			return getGroupPropertiesCompletions(camelCatalog);
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}
	
	protected CompletableFuture<List<CompletionItem>> getGroupPropertiesCompletions(CompletableFuture<CamelCatalog> camelCatalog) {
		return camelCatalog.thenApply(catalog -> {
			if (catalog instanceof DefaultCamelCatalog) {
				MainModel mainModel =  ((DefaultCamelCatalog)catalog).mainModel();
				String groupPrefix = CAMEL_KEY_PREFIX + propertyGroup + ".";
				return mainModel.getOptions().stream()
						.filter(option -> option.getName().startsWith(groupPrefix))
						.map(option -> {
							String realOptionName = option.getName().substring(groupPrefix.length());
							CompletionItem completionItem = new CompletionItem(realOptionName);
							completionItem.setDocumentation(option.getDescription());
							completionItem.setDeprecated(option.isDeprecated());
							completionItem.setInsertText(realOptionName + "=");
							return completionItem;
						}).collect(Collectors.toList());
			} else {
				return Collections.emptyList();
			}
		});
	}

	protected CompletableFuture<List<CompletionItem>> getTopLevelCamelCompletion(CompletableFuture<CamelCatalog> camelCatalog) {
		return camelCatalog.thenApply(catalog -> {
			MainModel mainModel = new Gson().fromJson(catalog.mainJsonSchema(), MainModel.class);
			List<CompletionItem> allCompletionItems = new ArrayList<>();
			allCompletionItems.addAll(createGroupCompletionFromMainModel(mainModel));
			allCompletionItems.add(createCompletionItemForCamelComponent());
			return allCompletionItems;
		});
	}

	private CompletionItem createCompletionItemForCamelComponent() {
		CompletionItem completionItem = new CompletionItem("component");
		completionItem.setInsertText("component.");
		return completionItem;
	}

	private List<CompletionItem> createGroupCompletionFromMainModel(MainModel mainModel) {
		return mainModel.getGroups().stream().map(group -> {
			String realGroupName = group.getName().replaceFirst(CAMEL_KEY_PREFIX, "");
			CompletionItem completionItem = new CompletionItem(realGroupName);
			completionItem.setDocumentation(group.getDescription());
			completionItem.setInsertText(realGroupName + ".");
			return completionItem;
		}).collect(Collectors.toList());
	}

	public String getCamelPropertyKey() {
		return camelPropertyKey;
	}

	public CamelComponentPropertyKey getCamelComponentPropertyKey() {
		return camelComponentPropertyKey;
	}

	public CamelPropertyEntryInstance getCamelPropertyEntryInstance() {
		return camelPropertyEntryInstance;
	}

	public int getLine() {
		return camelPropertyEntryInstance.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return camelPropertyEntryInstance.getStartPositionInLine();
	}

	@Override
	public int getEndPositionInLine() {
		return camelPropertyKey.length();
	}

	public CompletableFuture<Hover> getHover(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(camelComponentPropertyKey != null && camelComponentPropertyKey.isInRange(position.getCharacter())) {
			return camelComponentPropertyKey.getHover(position, camelCatalog);
		}
		return CompletableFuture.completedFuture(null);
	}

}
