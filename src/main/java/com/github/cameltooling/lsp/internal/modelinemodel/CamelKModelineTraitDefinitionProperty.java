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

public class CamelKModelineTraitDefinitionProperty implements ICamelKModelineOptionValue {

	private String traitDefinitionProperty;
	private int startPosition;
	private CamelKModelineTraitOption traitOption;
	private int line;

	public CamelKModelineTraitDefinitionProperty(CamelKModelineTraitOption camelKModelineTraitOption, String traitDefinitionProperty, int startPosition, int line) {
		this.traitOption = camelKModelineTraitOption;
		this.traitDefinitionProperty = traitDefinitionProperty;
		this.startPosition = startPosition;
		this.line = line;
	}
	
	public int getLine() {
		return line;
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + traitDefinitionProperty.length();
	}

	@Override
	public String getValueAsString() {
		return traitDefinitionProperty;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int position, CompletableFuture<CamelCatalog> camelCatalog) {
		String filter = retrieveTraitPropertyPartBefore(position);
		return CompletableFuture.completedFuture(CamelKTraitManager.getTraitPropertyNameCompletionItems(filter, this));
	}
	
	private String retrieveTraitPropertyPartBefore(int position) {
		return traitDefinitionProperty.substring(0, position - getStartPositionInLine());
	}
	
	@Override
	public CompletableFuture<Hover> getHover(int characterPosition, CompletableFuture<CamelCatalog> camelCatalog) {
		String description = CamelKTraitManager.getPropertyDescription(getTraitOption().getTraitDefinition().getValueAsString(), traitDefinitionProperty);
		if (description != null) {
			return CompletableFuture.completedFuture(createHover(description));
		}
		return ICamelKModelineOptionValue.super.getHover(characterPosition, camelCatalog);
	}

	public CamelKModelineTraitOption getTraitOption() {
		return traitOption;
	}

}
