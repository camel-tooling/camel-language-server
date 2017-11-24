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
package org.apache.camel.tools.lsp.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.tools.lsp.internal.completion.CamelEndpointCompletionProcessor;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lhein
 */
public class CamelTextDocumentService implements TextDocumentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CamelTextDocumentService.class);
	private Map<String, TextDocumentItem> openedDocuments = new HashMap<>();
	
	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(TextDocumentPositionParams completionRequest) {
		LOGGER.info("completion: " + completionRequest.getTextDocument().getUri());
		TextDocumentItem textDocumentItem = openedDocuments.get(completionRequest.getTextDocument().getUri());
		return new CamelEndpointCompletionProcessor(textDocumentItem).getCompletions(completionRequest.getPosition()).thenApply(Either::forLeft);
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		LOGGER.info("resolveCompletionItem: " + unresolved.getDetail());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
		LOGGER.info("hover: " + position.getTextDocument());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
		LOGGER.info("signatureHelp: " + position.getTextDocument());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<? extends Location>> definition(TextDocumentPositionParams position) {
		LOGGER.info("definition: " + position.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		LOGGER.info("references: " + params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams position) {
		LOGGER.info("documentHighlight: " + position.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends SymbolInformation>> documentSymbol(DocumentSymbolParams params) {
		LOGGER.info("documentSymbol: " + params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends Command>> codeAction(CodeActionParams params) {
		LOGGER.info("codeAction: " + params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		LOGGER.info("codeLens: " + params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
		LOGGER.info("resolveCodeLens: " + unresolved.getCommand().getCommand());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		LOGGER.info("formatting: " + params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		LOGGER.info("rangeFormatting: " + params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
		LOGGER.info("onTypeFormatting: " + params.getTextDocument());
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		LOGGER.info("rename: " + params.getTextDocument());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		TextDocumentItem textDocument = params.getTextDocument();
		LOGGER.info("didOpen: {0}", textDocument);
		openedDocuments.put(textDocument.getUri(), textDocument);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		LOGGER.info("didChange: " + params.getTextDocument());
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		LOGGER.info("didClose: " + params.getTextDocument());
		openedDocuments.remove(params.getTextDocument().getUri());
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		LOGGER.info("didSave: " + params.getTextDocument());
	}

}
