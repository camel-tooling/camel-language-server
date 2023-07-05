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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

/**
 * Represents one entry in properties file or in Camel K modeline. For instance, the whole entry "camel.component.timer.delay=1000"
 *
 */
public class CamelPropertyEntryInstance implements ILineRangeDefineable {
	
	private CamelPropertyKeyInstance camelPropertyKeyInstance;
	private CamelPropertyValueInstance camelPropertyValueInstance;
	private String line;
	private Position startPosition;
	private TextDocumentItem textDocumentItem;

	public CamelPropertyEntryInstance(String line, Position startPosition, TextDocumentItem textDocumentItem) {
		this.line = line;
		this.startPosition = startPosition;
		this.textDocumentItem = textDocumentItem;
		int indexOf = line.indexOf('=');
		String camelPropertyFileKeyInstanceString;
		String camelPropertyFileValueInstanceString;
		if (indexOf != -1) {
			camelPropertyFileKeyInstanceString = line.substring(0, indexOf);
			camelPropertyFileValueInstanceString = line.substring(indexOf+1);
		} else {
			camelPropertyFileKeyInstanceString = line;
			camelPropertyFileValueInstanceString = null;
		}
		camelPropertyKeyInstance = new CamelPropertyKeyInstance(camelPropertyFileKeyInstanceString, this);
		camelPropertyValueInstance = new CamelPropertyValueInstance(camelPropertyFileValueInstanceString, camelPropertyKeyInstance, textDocumentItem);
	}
	
	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, CompletableFuture<CamelCatalog> camelCatalog, SettingsManager settingsManager, KameletsCatalogManager kameletsCatalogManager) {
		if (isOnPropertyKey(position)) {
			return camelPropertyKeyInstance.getCompletions(position, camelCatalog);
		} else {
			return camelPropertyValueInstance.getCompletions(position, camelCatalog, kameletsCatalogManager);
		}
	}

	private boolean isOnPropertyKey(Position position) {
		return position.getCharacter() <= camelPropertyKeyInstance.getEndposition();
	}
	
	CamelPropertyKeyInstance getCamelPropertyKeyInstance() {
		return camelPropertyKeyInstance;
	}

	CamelPropertyValueInstance getCamelPropertyValueInstance() {
		return camelPropertyValueInstance;
	}

	public int getLine() {
		return startPosition.getLine();
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition.getCharacter();
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + line.length();
	}

	public CompletableFuture<Hover> getHover(Position position, CompletableFuture<CamelCatalog> camelCatalog, KameletsCatalogManager kameletCatalogManager) {
		if (isOnPropertyKey(position)) {
			return camelPropertyKeyInstance.getHover(position, camelCatalog);
		} else {
			return camelPropertyValueInstance.getHover(position, camelCatalog, kameletCatalogManager);
		}
	}

	public boolean shouldUseDashedCase() {
		return textDocumentItem != null
				&& new DashedCaseDetector().hasDashedCaseInCamelPropertyOption(textDocumentItem.getText());
	}

	public Collection<Diagnostic> validate(Set<CamelPropertyEntryInstance> allCamelPropertyEntriesOfTheFile) {
		if (camelPropertyKeyInstance != null) {
			return camelPropertyKeyInstance.validate(allCamelPropertyEntriesOfTheFile);
		}
		return Collections.emptyList();
	}

}
