/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.tools.lsp.internal.hover;

import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tools.lsp.internal.model.util.StringUtils;
import org.apache.camel.tools.lsp.internal.parser.ParserFileHelper;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HoverProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(HoverProcessor.class);
	private TextDocumentItem textDocumentItem;
	private CompletableFuture<CamelCatalog> camelCatalog;

	public HoverProcessor(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog) {
		this.textDocumentItem = textDocumentItem;
		this.camelCatalog = camelCatalog;
	}

	public CompletableFuture<Hover> getHover(Position position) {
		try {
			ParserFileHelper parserFileHelper = new ParserFileHelper();
			if(parserFileHelper.getCorrespondingCamelNodeForCompletion(textDocumentItem, position.getLine()) != null){
				String camelComponentUri = parserFileHelper.getCamelComponentUri(textDocumentItem, position);
				String componentName = StringUtils.asComponentName(camelComponentUri);
				if (componentName != null) {
					return camelCatalog.thenApply(new HoverFuture(componentName));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error searching hover", e);
		}
		return CompletableFuture.completedFuture(null);
	}

}
