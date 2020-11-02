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

/**
 * Represents the Modeline trait option.
 * For instance, "// camel-k: trait=quarkus.enabled=true", it represents "quarkus.enabled=true"
 *
 */
public class CamelKModelineTraitOption implements ICamelKModelineOptionValue {

	private int startPosition;
	private int endPosition;
	private String optionValue;
	private CamelKModelineTraitDefinition traitDefinition;
	private CamelKModelineTraitDefinitionProperty traitProperty;
	private int line;

	public CamelKModelineTraitOption(String optionValue, int startPosition, int line) {
		this.optionValue = optionValue;
		this.startPosition = startPosition;
		this.line = line;
		this.endPosition = startPosition + optionValue.length();
		this.traitDefinition = createTraitDefinition(optionValue, startPosition);
		this.traitProperty = createTraitProperty(optionValue);
	}

	private CamelKModelineTraitDefinitionProperty createTraitProperty(String optionValue) {
		int indexOfDotSeparator = optionValue.indexOf('.');
		if(indexOfDotSeparator != -1) {
			int indexOfEqualSeparator = optionValue.indexOf('=', indexOfDotSeparator);
			int startPositionOfTraitDefintionProperty = getStartPositionInLine() + indexOfDotSeparator + 1;
			if(indexOfEqualSeparator != -1) {
				return new CamelKModelineTraitDefinitionProperty(this, optionValue.substring(indexOfDotSeparator + 1, indexOfEqualSeparator), startPositionOfTraitDefintionProperty, line);
			} else {
				return new CamelKModelineTraitDefinitionProperty(this, optionValue.substring(indexOfDotSeparator + 1), startPositionOfTraitDefintionProperty, line);
			}
		}
		return null;
	}

	private CamelKModelineTraitDefinition createTraitDefinition(String optionValue, int startPosition) {
		int indexOfDotSeparator = optionValue.indexOf('.');
		if(indexOfDotSeparator != -1) {
			return new CamelKModelineTraitDefinition(this, optionValue.substring(0, indexOfDotSeparator), startPosition, line);
		} else {
			return new CamelKModelineTraitDefinition(this, optionValue, startPosition, line);
		}
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
		return endPosition;
	}

	@Override
	public String getValueAsString() {
		return optionValue;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(traitDefinition != null && traitDefinition.isInRange(position)) {
			return traitDefinition.getCompletions(position, camelCatalog);
		} else if(traitProperty!= null && traitProperty.isInRange(position)) {
			return traitProperty.getCompletions(position, camelCatalog);
		}
		return ICamelKModelineOptionValue.super.getCompletions(position, camelCatalog);
	}
	
	@Override
	public CompletableFuture<Hover> getHover(int characterPosition, CompletableFuture<CamelCatalog> camelCatalog) {
		if(traitDefinition != null && traitDefinition.isInRange(characterPosition)) {
			return traitDefinition.getHover(characterPosition, camelCatalog);
		} else if (traitProperty!= null && traitProperty.isInRange(characterPosition)) {
			return traitProperty.getHover(characterPosition, camelCatalog);
		}
		return ICamelKModelineOptionValue.super.getHover(characterPosition, camelCatalog);
	}

	public CamelKModelineTraitDefinition getTraitDefinition() {
		return traitDefinition;
	}

	public CamelKModelineTraitDefinitionProperty getTraitProperty() {
		return traitProperty;
	}

}
