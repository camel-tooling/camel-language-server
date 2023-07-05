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
package com.github.cameltooling.lsp.internal.completion;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyEntryInstance;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

public class CamelPropertiesCompletionProcessor {

	private TextDocumentItem textDocumentItem;
	private CompletableFuture<CamelCatalog> camelCatalog;

	public CamelPropertiesCompletionProcessor(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog) {
		this.textDocumentItem = textDocumentItem;
		this.camelCatalog = camelCatalog;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position, SettingsManager settingsManager, KameletsCatalogManager kameletsCatalogManager) {
		if (textDocumentItem != null) {
			String line = new ParserFileHelperUtil().getLine(textDocumentItem, position);
			return new CamelPropertyEntryInstance(line, new Position(position.getLine(), 0), textDocumentItem).getCompletions(position, camelCatalog, settingsManager, kameletsCatalogManager);
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}
	
}
