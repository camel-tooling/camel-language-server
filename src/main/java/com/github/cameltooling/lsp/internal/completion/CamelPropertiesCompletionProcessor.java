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

import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyFileEntryInstance;
import com.github.cameltooling.lsp.internal.parser.CamelKafkaConnectDSLParser;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public class CamelPropertiesCompletionProcessor {

	private TextDocumentItem textDocumentItem;
	private CompletableFuture<CamelCatalog> camelCatalog;

	public CamelPropertiesCompletionProcessor(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog) {
		this.textDocumentItem = textDocumentItem;
		this.camelCatalog = camelCatalog;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		if (textDocumentItem != null) {
			String line = new ParserFileHelperUtil().getLine(textDocumentItem, position);
			if (new CamelKafkaConnectDSLParser().isInsideACamelUri(line, position.getCharacter())) {
				return new CamelEndpointCompletionProcessor(textDocumentItem, camelCatalog).getCompletions(position);
			} else {
				return new CamelPropertyFileEntryInstance(camelCatalog, line).getCompletions(position.getCharacter());
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}
	
}
