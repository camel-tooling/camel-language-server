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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.CamelTextDocumentService;
import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public class UnknownPropertyQuickfix {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(UnknownPropertyQuickfix.class);
	private CamelTextDocumentService camelTextDocumentService;

	public UnknownPropertyQuickfix(CamelTextDocumentService camelTextDocumentService) {
		this.camelTextDocumentService = camelTextDocumentService;
	}

	public CompletableFuture<List<Either<Command, CodeAction>>> apply(CodeActionParams params) {
		return CompletableFuture.supplyAsync(() -> {
			TextDocumentItem openedDocument = camelTextDocumentService.getOpenedDocument(params.getTextDocument().getUri());
			List<Diagnostic> diagnostics = params.getContext().getDiagnostics();
			List<Either<Command, CodeAction>> res = new ArrayList<>();
			for (Diagnostic diagnostic : diagnostics) {
				CharSequence currentValueInError = retrieveCurrentErrorValue(openedDocument, diagnostic);
				if(DiagnosticService.ERROR_CODE_UNKNOWN_PROPERTIES.equals(diagnostic.getCode())) {
					List<String> possibleProperties = retrievePossibleProperties(openedDocument, camelTextDocumentService.getCamelCatalog(), diagnostic.getRange().getStart());
					int distanceThreshold = Math.round(currentValueInError.length() * 0.4f);
					LevenshteinDistance levenshteinDistance = new LevenshteinDistance(distanceThreshold);
					List<String> mostProbableProperties = possibleProperties.stream()
							.filter(possibleProperty -> levenshteinDistance.apply(possibleProperty, currentValueInError) != -1)
							.collect(Collectors.toList());
					for (String mostProbableProperty : mostProbableProperties) {
						res.add(Either.forRight(createCodeAction(params, diagnostic, mostProbableProperty)));
					}
				}
			}
			
			return res;
		});
	}

	private String retrieveCurrentErrorValue(TextDocumentItem openedDocument, Diagnostic diagnostic) {
		String line = new ParserFileHelperUtil().getLine(openedDocument, diagnostic.getRange().getStart().getLine());
		return line.substring(diagnostic.getRange().getStart().getCharacter(), diagnostic.getRange().getEnd().getCharacter());
	}

	private CodeAction createCodeAction(CodeActionParams params, Diagnostic diagnostic, String possibleProperty) {
		CodeAction codeAction = new CodeAction("Did you mean "+possibleProperty + "?");
		codeAction.setDiagnostics(Collections.singletonList(diagnostic));
		codeAction.setKind(CodeActionKind.QuickFix);
		Map<String, List<TextEdit>> changes = new HashMap<>();
		TextEdit textEdit = new TextEdit(diagnostic.getRange(), possibleProperty);
		changes.put(params.getTextDocument().getUri(), Arrays.asList(textEdit));
		codeAction.setEdit(new WorkspaceEdit(changes));
		return codeAction;
	}

	private List<String> retrievePossibleProperties(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog, Position position) {
		try {
			return new CamelEndpointCompletionProcessor(textDocumentItem, camelCatalog)
					.getCompletions(position)
					.thenApply(completionItems -> completionItems.stream().map(CompletionItem::getInsertText).collect(Collectors.toList()))
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

}
