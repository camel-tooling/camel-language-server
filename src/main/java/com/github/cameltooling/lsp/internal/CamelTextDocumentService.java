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
package com.github.cameltooling.lsp.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;
import com.github.cameltooling.lsp.internal.definition.DefinitionProcessor;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticService;
import com.github.cameltooling.lsp.internal.documentsymbol.DocumentSymbolProcessor;
import com.github.cameltooling.lsp.internal.hover.HoverProcessor;
import com.github.cameltooling.lsp.internal.references.ReferencesProcessor;

/**
 * @author lhein
 */
public class CamelTextDocumentService implements TextDocumentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CamelTextDocumentService.class);
	private Map<String, TextDocumentItem> openedDocuments = new HashMap<>();
	private CompletableFuture<CamelCatalog> camelCatalog;
	private CamelLanguageServer camelLanguageServer;

	public CamelTextDocumentService(CamelLanguageServer camelLanguageServer) {
		this.camelLanguageServer = camelLanguageServer;
		camelCatalog = CompletableFuture.supplyAsync(() -> new DefaultCamelCatalog(true));
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
		String uri = completionParams.getTextDocument().getUri();
		LOGGER.info("completion: {}", uri);
		TextDocumentItem textDocumentItem = openedDocuments.get(uri);
		return new CamelEndpointCompletionProcessor(textDocumentItem, camelCatalog).getCompletions(completionParams.getPosition()).thenApply(Either::forLeft);
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		LOGGER.info("resolveCompletionItem: {}", unresolved.getLabel());
		return CompletableFuture.completedFuture(unresolved);
	}

	@Override
	public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
		LOGGER.info("hover: {}", position.getTextDocument());
		TextDocumentItem textDocumentItem = openedDocuments.get(position.getTextDocument().getUri());
		return new HoverProcessor(textDocumentItem, camelCatalog).getHover(position.getPosition());
	}

	@Override
	public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
		LOGGER.info("signatureHelp: {}", position.getTextDocument());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams params) {
		TextDocumentIdentifier textDocument = params.getTextDocument();
		LOGGER.info("definition: {}", textDocument);
		TextDocumentItem textDocumentItem = openedDocuments.get(textDocument.getUri());
		return new DefinitionProcessor(textDocumentItem).getDefinitions(params.getPosition());
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		LOGGER.info("references: {}", params.getTextDocument());
		return new ReferencesProcessor(openedDocuments.get(params.getTextDocument().getUri())).getReferences(params.getPosition());
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams position) {
		LOGGER.info("documentHighlight: {}", position.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
		LOGGER.info("documentSymbol: {}", params.getTextDocument());
		return new DocumentSymbolProcessor(openedDocuments.get(params.getTextDocument().getUri())).getDocumentSymbols();
	}

	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		LOGGER.info("codeAction: {}", params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		LOGGER.info("codeLens: {}", params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
		LOGGER.info("resolveCodeLens: {}", unresolved.getCommand().getCommand());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		LOGGER.info("formatting: {}", params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		LOGGER.info("rangeFormatting: {}", params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
		LOGGER.info("onTypeFormatting: {}", params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		LOGGER.info("rename: {}", params.getTextDocument());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		TextDocumentItem textDocument = params.getTextDocument();
		LOGGER.info("didOpen: {}", textDocument);
		openedDocuments.put(textDocument.getUri(), textDocument);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		LOGGER.info("didChange: {}", params.getTextDocument());
		List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
		TextDocumentItem textDocumentItem = openedDocuments.get(params.getTextDocument().getUri());
		if (!contentChanges.isEmpty()) {
			textDocumentItem.setText(contentChanges.get(0).getText());
		}
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		LOGGER.info("didClose: {}", params.getTextDocument());
		openedDocuments.remove(params.getTextDocument().getUri());
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		LOGGER.info("didSave: {}", params.getTextDocument());
		new DiagnosticService(camelCatalog, camelLanguageServer).compute(params);
	}

	public TextDocumentItem getOpenedDocument(String uri) {
		return openedDocuments.get(uri);
	}
}
