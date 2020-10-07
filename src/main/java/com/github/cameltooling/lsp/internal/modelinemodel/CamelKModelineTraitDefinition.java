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
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;

import com.github.cameltooling.lsp.internal.completion.modeline.CamelKTraitManager;

public class CamelKModelineTraitDefinition implements ICamelKModelineOptionValue {

	private String traitDefinitionName;
	private int startPosition;
	private CamelKModelineTraitOption traitOption;

	public CamelKModelineTraitDefinition(CamelKModelineTraitOption traitOption, String traitDefinitionName, int startPosition) {
		this.traitOption = traitOption;
		this.traitDefinitionName = traitDefinitionName;
		this.startPosition = startPosition;
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + traitDefinitionName.length();
	}

	@Override
	public String getValueAsString() {
		return traitDefinitionName;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int position, CompletableFuture<CamelCatalog> camelCatalog) {
		String filter = retrieveTraitDefinitionPartBefore(position);
		return CompletableFuture.completedFuture(CamelKTraitManager.getTraitDefinitionNameCompletionItems(filter, this));
	}
	
	private String retrieveTraitDefinitionPartBefore(int position) {
		return traitDefinitionName.substring(0, position - getStartPositionInLine());
	}
	
	@Override
	public CompletableFuture<Hover> getHover(int characterPosition, CompletableFuture<CamelCatalog> camelCatalog) {
		String description = CamelKTraitManager.getDescription(traitDefinitionName);
		if (description != null) {
			return CompletableFuture.completedFuture(createHover(description));
		}
		return ICamelKModelineOptionValue.super.getHover(characterPosition, camelCatalog);
	}

	public CamelKModelineTraitOption getTraitOption() {
		return traitOption;
	}

}
