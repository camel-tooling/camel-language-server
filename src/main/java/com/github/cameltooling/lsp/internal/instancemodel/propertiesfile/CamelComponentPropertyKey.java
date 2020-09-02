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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;

import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

/**
 * Represents the subpart of the key after camel.component.
 * For instance, with "camel.component.timer.delay=1000",
 * it is used to represents "timer.delay"
 * 
 */
public class CamelComponentPropertyKey implements ILineRangeDefineable {

	private String fullCamelComponentPropertyKey;
	private CamelComponentNamePropertyInstance componentName;
	private CamelComponentParameterPropertyInstance componentProperty;
	private CamelPropertyKeyInstance camelPropertyKeyInstance;

	public CamelComponentPropertyKey(String camelComponentPropertyKey, CamelPropertyKeyInstance camelPropertyKeyInstance) {
		this.fullCamelComponentPropertyKey = camelComponentPropertyKey;
		this.camelPropertyKeyInstance = camelPropertyKeyInstance;
		int firstDotIndex = camelComponentPropertyKey.indexOf('.');
		if(firstDotIndex != -1) {
			componentName = new CamelComponentNamePropertyInstance(camelComponentPropertyKey.substring(0, firstDotIndex), this);
			componentProperty = new CamelComponentParameterPropertyInstance(camelComponentPropertyKey.substring(firstDotIndex+1), componentName.getEndPositionInLine() + 1, this);
		} else {
			componentName = new CamelComponentNamePropertyInstance(camelComponentPropertyKey, this);
		}
	}

	public boolean isInRange(int positionChar) {
		return getStartPositionInLine() <= positionChar
				&& positionChar <= fullCamelComponentPropertyKey.length() + getStartPositionInLine();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		int characterPosition = position.getCharacter();
		if(isInside(componentName, characterPosition)) {
			return componentName.getCompletions(position, camelCatalog);
		} else if(isInside(componentProperty, characterPosition)){
			return componentProperty.getCompletions(position, camelCatalog);
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
	}

	private boolean isInside(ILineRangeDefineable lineRangeDefineable, int characterPosition) {
		return lineRangeDefineable != null
				&& lineRangeDefineable.getStartPositionInLine() <= characterPosition
				&& lineRangeDefineable.getEndPositionInLine() >= characterPosition;
	}

	@Override
	public int getStartPositionInLine() {
		return camelPropertyKeyInstance.getStartPositionInLine() + CamelPropertyKeyInstance.CAMEL_COMPONENT_KEY_PREFIX.length();
	}

	public String getComponentId() {
		return componentName.getName();
	}

	public String getComponentProperty() {
		return componentProperty != null ? componentProperty.getProperty() : null;
	}

	@Override
	public int getLine() {
		return getCamelPropertyKeyInstance().getLine();
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + fullCamelComponentPropertyKey.length();
	}

	public CamelPropertyKeyInstance getCamelPropertyKeyInstance() {
		return camelPropertyKeyInstance;
	}

	public CompletableFuture<Hover> getHover(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		int characterPosition = position.getCharacter();
		if(isInside(componentName, characterPosition)) {
			return componentName.getHover(camelCatalog);
		} else if(isInside(componentProperty, characterPosition)){
			return componentProperty.getHover(camelCatalog);
		} else {
			return CompletableFuture.completedFuture(null);
		}
	}

	public boolean shouldUseDashedCase() {
		return camelPropertyKeyInstance.shouldUseDashedCase();
	}

}
