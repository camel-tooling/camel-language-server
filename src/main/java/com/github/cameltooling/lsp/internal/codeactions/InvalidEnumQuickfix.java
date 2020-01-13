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
package com.github.cameltooling.lsp.internal.codeactions;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.CamelTextDocumentService;
import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;

public class InvalidEnumQuickfix extends AbstractQuickfix {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(InvalidEnumQuickfix.class);

	public InvalidEnumQuickfix(CamelTextDocumentService camelTextDocumentService) {
		super(camelTextDocumentService);
	}

	@Override
	protected List<String> retrievePossibleValues(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog, Position position) {
		try {
			return new CamelEndpointCompletionProcessor(textDocumentItem, camelCatalog)
					.getCompletions(position)
					.thenApply(completionItems -> completionItems.stream().map(CompletionItem::getLabel).collect(Collectors.toList()))
					.get();
		} catch (InterruptedException e) {
			LOGGER.error("Interruption while computing possible properties for quickfix", e);
			Thread.currentThread().interrupt();
			return Collections.emptyList();
		} catch (ExecutionException e) {
			LOGGER.error("Exception while computing possible properties for quickfix", e);
			return Collections.emptyList();
		}
	}

	@Override
	protected String getDiagnosticId() {
		return DiagnosticService.ERROR_CODE_INVALID_ENUM;
	}


}
