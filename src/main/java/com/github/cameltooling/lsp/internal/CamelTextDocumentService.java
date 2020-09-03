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
package com.github.cameltooling.lsp.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.maven.MavenVersionManager;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentHighlightParams;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SignatureHelpParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.codeactions.InvalidEnumQuickfix;
import com.github.cameltooling.lsp.internal.codeactions.UnknownPropertyQuickfix;
import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.CamelPropertiesCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineCompletionprocessor;
import com.github.cameltooling.lsp.internal.definition.DefinitionProcessor;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticRunner;
import com.github.cameltooling.lsp.internal.documentsymbol.DocumentSymbolProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelKModelineHoverProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelPropertiesFileHoverProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelURIHoverProcessor;
import com.github.cameltooling.lsp.internal.parser.CamelKModelineParser;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;
import com.github.cameltooling.lsp.internal.references.ReferencesProcessor;
import com.github.cameltooling.lsp.internal.settings.JSONUtility;
import com.google.gson.Gson;

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
	
	public void updateCatalog(String camelVersion, List<Map<?,?>> extraComponents) {
		camelCatalog = CompletableFuture.supplyAsync(() -> {
			DefaultCamelCatalog catalog = new DefaultCamelCatalog(true);
			if (camelVersion != null && !camelVersion.isEmpty()) {
				catalog.setVersionManager(new MavenVersionManager());
				if (!catalog.loadVersion(camelVersion)) {
					LOGGER.warn("Cannot load Camel catalog with version {}", camelVersion);
				}
			}
			if (extraComponents != null) {
				for (Map<?,?> extraComponent : extraComponents) {
					JSONUtility jsonUtility = new JSONUtility();
					Map<?,?> extraComponentTopLevel = jsonUtility.toModel(extraComponent, Map.class);
					Map<?,?> componentAttributes = jsonUtility.toModel(extraComponentTopLevel.get("component"), Map.class);
					String name = (String) componentAttributes.get("scheme");
					String className = (String) componentAttributes.get("javaType");
					catalog.addComponent(name, className, new Gson().toJson(extraComponent));
				}
			}
			return catalog;
		});
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
		String uri = completionParams.getTextDocument().getUri();
		LOGGER.info("completion: {}", uri);
		TextDocumentItem textDocumentItem = openedDocuments.get(uri);
		if (uri.endsWith(".properties")){
			return new CamelPropertiesCompletionProcessor(textDocumentItem, getCamelCatalog()).getCompletions(completionParams.getPosition()).thenApply(Either::forLeft);
		} else if(isOnCamelKModeline(completionParams.getPosition().getLine(), textDocumentItem)){
			return new CamelKModelineCompletionprocessor(textDocumentItem, getCamelCatalog()).getCompletions(completionParams.getPosition()).thenApply(Either::forLeft);
		} else {
			return new CamelEndpointCompletionProcessor(textDocumentItem, getCamelCatalog()).getCompletions(completionParams.getPosition()).thenApply(Either::forLeft);
		}
	}

	private boolean isOnCamelKModeline(int line, TextDocumentItem textDocumentItem) {
		return line == 0 && new CamelKModelineParser().retrieveModelineCamelKStart(new ParserFileHelperUtil().getLine(textDocumentItem, 0)) != null;
	}

	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		LOGGER.info("resolveCompletionItem: {}", unresolved.getLabel());
		return CompletableFuture.completedFuture(unresolved);
	}

	@Override
	public CompletableFuture<Hover> hover(HoverParams hoverParams) {
		LOGGER.info("hover: {}", hoverParams.getTextDocument());
		String uri = hoverParams.getTextDocument().getUri();
		TextDocumentItem textDocumentItem = openedDocuments.get(uri);
		if (uri.endsWith(".properties")){
			return new CamelPropertiesFileHoverProcessor(textDocumentItem).getHover(hoverParams.getPosition(), getCamelCatalog());
		} else if(isOnCamelKModeline(hoverParams.getPosition().getLine(), textDocumentItem)) {
			return new CamelKModelineHoverProcessor(textDocumentItem).getHover(hoverParams.getPosition().getCharacter(), getCamelCatalog());
		} else {
			return new CamelURIHoverProcessor(textDocumentItem, getCamelCatalog()).getHover(hoverParams.getPosition());
		}
	}

	@Override
	public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams signatureHelpParams) {
		LOGGER.info("signatureHelp: {}", signatureHelpParams.getTextDocument());
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
		TextDocumentIdentifier textDocument = params.getTextDocument();
		LOGGER.info("definition: {}", textDocument);
		TextDocumentItem textDocumentItem = openedDocuments.get(textDocument.getUri());
		return new DefinitionProcessor(textDocumentItem).getDefinitions(params.getPosition());
	}

	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		LOGGER.info("references: {}", params.getTextDocument());
		return new ReferencesProcessor(this, openedDocuments.get(params.getTextDocument().getUri())).getReferences(params.getPosition());
	}

	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(DocumentHighlightParams position) {
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
		CodeActionContext context = params.getContext();
		if (context != null && (context.getOnly() == null || context.getOnly().contains(CodeActionKind.QuickFix))) {
			return CompletableFuture.supplyAsync(() -> {
				List<Either<Command, CodeAction>> allQuickfixes = new ArrayList<>();
				allQuickfixes.addAll(new UnknownPropertyQuickfix(this).apply(params));
				allQuickfixes.addAll(new InvalidEnumQuickfix(this).apply(params));
				return allQuickfixes;
			});
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
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
		new DiagnosticRunner(getCamelCatalog(), camelLanguageServer).compute(params);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		LOGGER.info("didChange: {}", params.getTextDocument());
		List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
		TextDocumentItem textDocumentItem = openedDocuments.get(params.getTextDocument().getUri());
		if (!contentChanges.isEmpty()) {
			textDocumentItem.setText(contentChanges.get(0).getText());
			new DiagnosticRunner(getCamelCatalog(), camelLanguageServer).compute(params);
		}
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		LOGGER.info("didClose: {}", params.getTextDocument());
		String uri = params.getTextDocument().getUri();
		openedDocuments.remove(uri);
		/* The rule observed by VS Code servers as explained in LSP specification is to clear the Diagnostic when it is related to a single file.
		 * https://microsoft.github.io/language-server-protocol/specification#textDocument_publishDiagnostics
		 * */
		new DiagnosticRunner(getCamelCatalog(), camelLanguageServer).clear(uri);
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		LOGGER.info("didSave: {}", params.getTextDocument());
		new DiagnosticRunner(getCamelCatalog(), camelLanguageServer).compute(params);
	}

	public TextDocumentItem getOpenedDocument(String uri) {
		return openedDocuments.get(uri);
	}
	
	public Collection<TextDocumentItem> getAllOpenedDocuments() {
		return openedDocuments.values();
	}

	/**
	 * /!\ public for test purpose
	 * @return a Future of the Camel Catalog
	 */
	public CompletableFuture<CamelCatalog> getCamelCatalog() {
		return camelCatalog;
	}
}
