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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.completion.CamelComponentIdsCompletionsFuture;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

/**
 * Represents the subpart component key. For instance, with
 * "camel.component.timer.delay=1000", it is used to represents "timer"
 * 
 */
public class CamelComponentNamePropertyInstance implements ILineRangeDefineable {

	private String componentName;
	private CamelComponentPropertyKey camelComponentPropertyKey;

	public CamelComponentNamePropertyInstance(String componentName,
			CamelComponentPropertyKey camelComponentPropertyKey) {
		this.componentName = componentName;
		this.camelComponentPropertyKey = camelComponentPropertyKey;
	}

	@Override
	public int getLine() {
		return camelComponentPropertyKey.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return camelComponentPropertyKey.getStartPositionInLine();
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + componentName.length();
	}

	public String getName() {
		return componentName;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		int characterPosition = position.getCharacter();
		String componentIdBeforePosition = componentName.substring(0, characterPosition - getStartPositionInLine());
		return camelCatalog.thenApply(new CamelComponentIdsCompletionsFuture(this, componentIdBeforePosition));
	}

	public CompletableFuture<Hover> getHover(CompletableFuture<CamelCatalog> camelCatalog) {
		return camelCatalog.thenApply(catalog -> {
			String componentJSonSchema = catalog.componentJSonSchema(componentName);
			if (componentJSonSchema != null) {
				ComponentModel componentModel = ModelHelper.generateComponentModel(componentJSonSchema, true);
				if (componentModel != null) {
					String description = componentModel.getDescription();
					if (description != null) {
						return createHover(description);
					}
				}
			}
			return null;
		});
	}

}
