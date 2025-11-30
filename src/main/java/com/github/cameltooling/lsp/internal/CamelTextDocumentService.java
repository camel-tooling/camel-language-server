/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineInsertionProcessor;
import com.github.cameltooling.lsp.internal.parser.CamelKModelineInsertionParser;
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
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
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

import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.codeactions.CodeActionProcessor;
import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.CamelPropertiesCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.PomCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.VSCodeTasksCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineCompletionprocessor;
import com.github.cameltooling.lsp.internal.definition.DefinitionProcessor;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticRunner;
import com.github.cameltooling.lsp.internal.documentsymbol.DocumentSymbolProcessor;
import com.github.cameltooling.lsp.internal.folding.FoldingRangeProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelKModelineHoverProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelPropertiesFileHoverProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelURIHoverProcessor;
import com.github.cameltooling.lsp.internal.parser.CamelKModelineParser;
import com.github.cameltooling.lsp.internal.references.ReferencesProcessor;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;
import com.github.cameltooling.lsp.internal.telemetry.TelemetryLanguage;

/**
 * @author lhein
 */
public class CamelTextDocumentService implements TextDocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelTextDocumentService.class);
    protected Map<String, TextDocumentItem> openedDocuments = new HashMap<>();
    private CompletableFuture<CamelCatalog> camelCatalog;
    private CamelLanguageServer camelLanguageServer;
    private KameletsCatalogManager kameletsCatalogManager = new KameletsCatalogManager();
    
    private CamelCatalogService catalogService = new CamelCatalogService();

    public CamelTextDocumentService(CamelLanguageServer camelLanguageServer) {
        this.camelLanguageServer = camelLanguageServer;
        camelCatalog = CompletableFuture.supplyAsync(() -> new DefaultCamelCatalog(true));
    }
    
    public void updateCatalog(String camelVersion, String camelCatalogRuntimeProvider, List<Map<?,?>> extraComponents) {
        this.camelCatalog = catalogService.updateCatalog(camelVersion, camelCatalogRuntimeProvider, extraComponents);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams completionParams) {
        String uri = completionParams.getTextDocument().getUri();
        LOGGER.info("completion: {}", uri);
        TextDocumentItem textDocumentItem = openedDocuments.get(uri);

        if (textDocumentItem != null) {
            if (uri.endsWith(".properties")){
                return new CamelPropertiesCompletionProcessor(textDocumentItem, getCamelCatalog()).getCompletions(completionParams.getPosition(), getSettingsManager(), getKameletsCatalogManager()).thenApply(Either::forLeft);
            } else if (new CamelKModelineInsertionParser(textDocumentItem).canPutCamelKModeline(completionParams.getPosition())){
                return new CamelKModelineInsertionProcessor(textDocumentItem).getCompletions().thenApply(Either::forLeft);
            } else if (new CamelKModelineParser().isOnCamelKModeline(completionParams.getPosition().getLine(), textDocumentItem)){
                return new CamelKModelineCompletionprocessor(textDocumentItem, getCamelCatalog()).getCompletions(completionParams.getPosition()).thenApply(Either::forLeft);
            } else if(uri.endsWith("pom.xml")) {
                return new PomCompletionProcessor(textDocumentItem, getCamelCatalog()).getCompletions(completionParams.getPosition()).thenApply(Either::forLeft);
            } else if(uri.endsWith("tasks.json")) {
                return new VSCodeTasksCompletionProcessor(textDocumentItem).getCompletions(completionParams.getPosition()).thenApply(Either::forLeft);
            } else {
                return new CamelEndpointCompletionProcessor(textDocumentItem, getCamelCatalog(), getKameletsCatalogManager()).getCompletions(completionParams.getPosition(), getSettingsManager()).thenApply(Either::forLeft);
            }
        } else {
            LOGGER.warn("The document with uri {} has not been found in opened documents. Cannot provide completion.", uri);
            return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
        }
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
            return new CamelPropertiesFileHoverProcessor(textDocumentItem).getHover(hoverParams.getPosition(), getCamelCatalog(), getKameletsCatalogManager());
        } else if(new CamelKModelineParser().isOnCamelKModeline(hoverParams.getPosition().getLine(), textDocumentItem)) {
            return new CamelKModelineHoverProcessor(textDocumentItem).getHover(hoverParams.getPosition(), getCamelCatalog());
        } else {
            return new CamelURIHoverProcessor(textDocumentItem, getCamelCatalog(), getKameletsCatalogManager()).getHover(hoverParams.getPosition());
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
        return new CodeActionProcessor(this).getCodeActions(params);
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
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
        TextDocumentIdentifier textDocument = params.getTextDocument();
        LOGGER.info("foldingRange: {}", textDocument);
        String uri = textDocument.getUri();
        TextDocumentItem textDocumentItem = openedDocuments.get(uri);
        if (textDocumentItem != null) {
            return new FoldingRangeProcessor().computeFoldingRanges(textDocumentItem);
        } else {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem textDocument = params.getTextDocument();
        LOGGER.info("didOpen: {}", textDocument);
        openedDocuments.put(textDocument.getUri(), textDocument);
        new DiagnosticRunner(getCamelCatalog(), camelLanguageServer).compute(params);
        new TelemetryLanguage(camelLanguageServer.getTelemetryManager()).compute(textDocument);
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

    public CompletableFuture<CamelCatalog> getCamelCatalog() {
        return camelCatalog;
    }

    public SettingsManager getSettingsManager() {
        return camelLanguageServer.getSettingsManager();
    }
    
    public KameletsCatalogManager getKameletsCatalogManager() {
        return kameletsCatalogManager;
    }
}