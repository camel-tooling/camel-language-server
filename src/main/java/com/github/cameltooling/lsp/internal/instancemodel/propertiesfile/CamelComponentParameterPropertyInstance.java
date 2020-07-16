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
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.model.ComponentOptionModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.completion.CamelComponentOptionNamesCompletionFuture;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

/**
 * Represents the subpart component parameter in key. For instance, with
 * "camel.component.timer.delay=1000", it is used to represents "delay"
 * 
 */
public class CamelComponentParameterPropertyInstance implements ILineRangeDefineable {

	private String componentParameter;
	private CamelComponentPropertyKey camelComponentPropertykey;
	private int startCharacterInLine;

	public CamelComponentParameterPropertyInstance(String componentParameter, int startCharacterInLine,
			CamelComponentPropertyKey camelComponentPropertyKey) {
		this.componentParameter = componentParameter;
		this.startCharacterInLine = startCharacterInLine;
		this.camelComponentPropertykey = camelComponentPropertyKey;
	}

	@Override
	public int getLine() {
		return camelComponentPropertykey.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return startCharacterInLine;
	}

	@Override
	public int getEndPositionInLine() {
		return startCharacterInLine + componentParameter.length();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position,
			CompletableFuture<CamelCatalog> camelCatalog) {
		CamelPropertyValueInstance camelPropertyFileValueInstance = camelComponentPropertykey
				.getCamelPropertyKeyInstance().getCamelPropertyEntryInstance().getCamelPropertyValueInstance();
		String startComponentProperty = componentParameter.substring(0,
				position.getCharacter() - getStartPositionInLine());
		return camelCatalog
				.thenApply(new CamelComponentOptionNamesCompletionFuture(camelComponentPropertykey.getComponentId(),
						this, camelPropertyFileValueInstance, startComponentProperty));
	}

	public String getProperty() {
		return componentParameter;
	}

	public CompletableFuture<Hover> getHover(CompletableFuture<CamelCatalog> camelCatalog) {
		return camelCatalog.thenApply(catalog -> {
			String componentJSonSchema = catalog.componentJSonSchema(camelComponentPropertykey.getComponentId());
			if (componentJSonSchema != null) {
				ComponentModel componentModel = ModelHelper.generateComponentModel(componentJSonSchema, true);
				if (componentModel != null) {
					ComponentOptionModel componentOptionModel = componentModel.getComponentOption(getProperty());
					if (componentOptionModel != null) {
						String description = componentOptionModel.getDescription();
						if (description != null) {
							Hover hover = new Hover();
							hover.setContents(Collections.singletonList((Either.forLeft(description))));
							return hover;
						}
					}
				}
			}
			return null;
		});
	}

}
