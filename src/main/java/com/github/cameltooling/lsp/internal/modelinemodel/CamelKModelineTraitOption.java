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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.completion.modeline.CamelKTraitManager;

public class CamelKModelineTraitOption implements ICamelKModelineOptionValue {

	private int startPosition;
	private int endPosition;
	private String optionValue;
	private String traitDefinitionName;
	private String traitPropertyName;

	public CamelKModelineTraitOption(String optionValue, int startPosition) {
		this.optionValue = optionValue;
		this.startPosition = startPosition;
		this.endPosition = startPosition + optionValue.length();
		this.traitDefinitionName = computeTraitDefinitionName(optionValue);
		this.traitPropertyName = computeTraitPropertyName(optionValue);
	}

	private String computeTraitPropertyName(String optionValue) {
		int indexOfDotSeparator = optionValue.indexOf('.');
		if(indexOfDotSeparator != -1) {
			int indexOfEqualSeparator = optionValue.indexOf('=', indexOfDotSeparator);
			if(indexOfEqualSeparator != -1) {
				return optionValue.substring(indexOfDotSeparator + 1, indexOfEqualSeparator);
			}
		}
		return null;
	}

	private String computeTraitDefinitionName(String optionValue) {
		int indexOfDotSeparator = optionValue.indexOf('.');
		if(indexOfDotSeparator != -1) {
			return optionValue.substring(0,indexOfDotSeparator);
		} else {
			return null;
		}
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return endPosition;
	}

	@Override
	public String getValueAsString() {
		return optionValue;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(isInTraitDefinitionName(position)) {
			String filter = retrieveTraitDefinitionPartBefore(position);
			return CompletableFuture.completedFuture(CamelKTraitManager.getTraitDefinitionNameCompletionItems(filter));
		} else if(isInTraitPropertyName(position)) {
			String filter = retrieveTraitPropertyPartBefore(position);
			return CompletableFuture.completedFuture(CamelKTraitManager.getTraitPropertyNameCompletionItems(traitDefinitionName, filter));
		}
		return ICamelKModelineOptionValue.super.getCompletions(position, camelCatalog);
	}

	private String retrieveTraitPropertyPartBefore(int position) {
		return optionValue.substring(traitDefinitionName.length() + 1, position - getStartPositionInLine());
	}

	private String retrieveTraitDefinitionPartBefore(int position) {
		return (traitDefinitionName != null ? traitDefinitionName : optionValue).substring(0, position - getStartPositionInLine());
	}

	private boolean isInTraitDefinitionName(int position) {
		return getStartPositionInLine() <= position && position <= getStartPositionInLine() + (traitDefinitionName != null ? traitDefinitionName.length() : optionValue.length());
	}

	private boolean isInTraitPropertyName(int position) {
		return traitDefinitionName != null && getStartPositionInLine() + traitDefinitionName.length() + 1 <= position;
	}
	
	@Override
	public CompletableFuture<Hover> getHover(int characterPosition, CompletableFuture<CamelCatalog> camelCatalog) {
		if(getStartPositionInLine() + traitDefinitionName.length() >= characterPosition) {
			String description = CamelKTraitManager.getDescription(traitDefinitionName);
			if (description != null) {
				Hover hover = new Hover();
				hover.setContents(Collections.singletonList((Either.forLeft(description))));
				return CompletableFuture.completedFuture(hover);
			}
		}
		return ICamelKModelineOptionValue.super.getHover(characterPosition, camelCatalog);
	}

	public String getTraitDefinitionName() {
		return traitDefinitionName;
	}

	public String getTraitPropertyName() {
		return traitPropertyName;
	}

}
