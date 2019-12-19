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
import org.eclipse.lsp4j.Position;

import com.github.cameltooling.lsp.internal.completion.CamelComponentIdsCompletionsFuture;
import com.github.cameltooling.lsp.internal.completion.CamelComponentOptionNamesCompletionFuture;

/**
 * Represents the subpart component key.
 * For instance, with "camel.component.timer.delay=1000",
 * it is used to represents "timer.delay"
 * 
 */
public class CamelComponentPropertyFilekey {

	private CompletableFuture<CamelCatalog> camelCatalog;
	private String fullCamelComponentPropertyFileKey;
	private String componentId;
	private String componentProperty;
	private CamelPropertyFileKeyInstance camelPropertyFileKeyInstance;

	public CamelComponentPropertyFilekey(CompletableFuture<CamelCatalog> camelCatalog, String camelComponentPropertyFileKey, CamelPropertyFileKeyInstance camelPropertyFileKeyInstance) {
		this.camelCatalog = camelCatalog;
		this.fullCamelComponentPropertyFileKey = camelComponentPropertyFileKey;
		this.camelPropertyFileKeyInstance = camelPropertyFileKeyInstance;
		int firstDotIndex = camelComponentPropertyFileKey.indexOf('.');
		if(firstDotIndex != -1) {
			componentId = camelComponentPropertyFileKey.substring(0, firstDotIndex);
			componentProperty = camelComponentPropertyFileKey.substring(firstDotIndex+1);
		} else {
			componentId = camelComponentPropertyFileKey;
		}
	}

	public boolean isInRange(int positionChar) {
		return getStartPositionInLine() <= positionChar
				&& positionChar <= fullCamelComponentPropertyFileKey.length() + getStartPositionInLine();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		int characterPosition = position.getCharacter();
		if(isInsideComponentId(characterPosition)) {
			String componentIdBeforePosition = fullCamelComponentPropertyFileKey.substring(0, characterPosition - getStartPositionInLine());
			return camelCatalog.thenApply(new CamelComponentIdsCompletionsFuture(componentIdBeforePosition));
		} else if(isInsideComponentProperty(characterPosition)){
			CamelPropertyFileValueInstance camelPropertyFileValueInstance = camelPropertyFileKeyInstance.getCamelPropertyFileEntryInstance().getCamelPropertyFileValueInstance();
			String startComponentProperty = fullCamelComponentPropertyFileKey.substring(componentId.length() + 1, characterPosition - getStartPositionInLine());
			return camelCatalog.thenApply(new CamelComponentOptionNamesCompletionFuture(componentId, camelPropertyFileValueInstance, startComponentProperty));
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
	}

	private boolean isInsideComponentProperty(int characterPosition) {
		return componentId != null
				&& isAfterComponentIdAndDot(characterPosition)
				&& isInsideKeyAndBeforeNextDot(characterPosition);
	}

	private boolean isInsideKeyAndBeforeNextDot(int characterPosition) {
		return componentProperty == null
				|| componentProperty.indexOf('.') == -1
				|| componentProperty.indexOf('.') + 1 + getStartPositionInLine() + componentId.length() + 1 >= characterPosition;
	}

	private boolean isAfterComponentIdAndDot(int characterPosition) {
		return getStartPositionInLine() + componentId.length() + 1 <= characterPosition;
	}


	private boolean isInsideComponentId(int characterPosition) {
		return getStartPositionInLine() <= characterPosition
				&& componentId != null
				&& getStartPositionInLine() + componentId.length() >= characterPosition;
	}
	
	private int getStartPositionInLine() {
		return CamelPropertyFileKeyInstance.CAMEL_COMPONENT_KEY_PREFIX.length();
	}

	public String getComponentId() {
		return componentId;
	}

	public String getComponentProperty() {
		return componentProperty;
	}

}
