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
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.maven.MavenVersionManager;
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
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.Position;
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

import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.CamelRuntimeProvider;
import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.RuntimeConfigurationProperties;
import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.RuntimeConfigurationPropertiesLoader;
import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.codeactions.CodeActionProcessor;
import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.CamelPropertiesCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.CamelYamlDslKeywordProcessor;
import com.github.cameltooling.lsp.internal.completion.CamelYamlParametersCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.PomCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.VSCodeTasksCompletionProcessor;
import com.github.cameltooling.lsp.internal.completion.YamlDslLines;
import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineCompletionprocessor;
import com.github.cameltooling.lsp.internal.definition.DefinitionProcessor;
import com.github.cameltooling.lsp.internal.diagnostic.DiagnosticRunner;
import com.github.cameltooling.lsp.internal.documentsymbol.DocumentSymbolProcessor;
import com.github.cameltooling.lsp.internal.folding.FoldingRangeProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelKModelineHoverProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelPropertiesFileHoverProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelURIHoverProcessor;
import com.github.cameltooling.lsp.internal.hover.CamelXmlEipHoverProcessor;
import com.github.cameltooling.lsp.internal.parser.CamelKModelineParser;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelper;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperFactory;
import com.github.cameltooling.lsp.internal.references.ReferencesProcessor;
import com.github.cameltooling.lsp.internal.settings.JSONUtility;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;
import com.github.cameltooling.lsp.internal.telemetry.TelemetryLanguage;
import com.google.gson.Gson;

/**
 * @author lhein
 */
public class CamelTextDocumentService implements TextDocumentService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CamelTextDocumentService.class);
	protected Map<String, TextDocumentItem> openedDocuments = new HashMap<>();
	private CompletableFuture<CamelCatalog> camelCatalog;
	private CamelLanguageServer camelLanguageServer;
	private KameletsCatalogManager kameletsCatalogManager = new KameletsCatalogManager();

	public CamelTextDocumentService(CamelLanguageServer camelLanguageServer) {
		this.camelLanguageServer = camelLanguageServer;
		camelCatalog = CompletableFuture.supplyAsync(() -> {
			DefaultCamelCatalog catalog = new DefaultCamelCatalog(true);
			registerConfigurationProperties(catalog, RuntimeConfigurationProperties.NONE);
			return catalog;
		});
	}
	
	public void updateCatalog(String camelVersion, String camelCatalogRuntimeProvider, String camelCatalogRuntimeProviderVersion, List<Map<?,?>> extraComponents) {
		logToClient(MessageType.Info, String.format("Updating Camel catalog - requested version: %s, runtime provider: %s, runtime provider version: %s",
				camelVersion, camelCatalogRuntimeProvider, camelCatalogRuntimeProviderVersion));
		ClassLoader callerClassLoader = Thread.currentThread().getContextClassLoader();
		camelCatalog = CompletableFuture.supplyAsync(() -> {
			Thread.currentThread().setContextClassLoader(callerClassLoader);
			DefaultCamelCatalog catalog = new DefaultCamelCatalog(true);
			updateCatalogVersion(camelVersion, catalog);
			RuntimeConfigurationProperties runtimeProperties = updateCatalogRuntimeProvider(camelCatalogRuntimeProvider, camelCatalogRuntimeProviderVersion, catalog);
			updateCatalogExtraComponents(extraComponents, catalog);
			registerConfigurationProperties(catalog, runtimeProperties);
			String loadedRuntimeProviderVersion = catalog.getRuntimeProviderLoadedVersion();
			logToClient(MessageType.Info, String.format("Camel catalog updated - active version: %s, runtime provider: %s%s",
					catalog.getCatalogVersion(),
					camelCatalogRuntimeProvider != null ? camelCatalogRuntimeProvider : "DEFAULT",
					loadedRuntimeProviderVersion != null ? " " + loadedRuntimeProviderVersion : ""));
			return catalog;
		});
	}

	private void updateCatalogExtraComponents(List<Map<?, ?>> extraComponents, DefaultCamelCatalog catalog) {
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
	}

	/**
	 * @return the configuration properties that the requested version of the runtime adds to the ones of Camel Main
	 */
	private RuntimeConfigurationProperties updateCatalogRuntimeProvider(String camelCatalogRuntimeProvider, String camelCatalogRuntimeProviderVersion, DefaultCamelCatalog catalog) {
		if(camelCatalogRuntimeProvider != null && !camelCatalogRuntimeProvider.isEmpty()) {
			RuntimeProvider runtimeProvider = CamelRuntimeProvider.getProvider(camelCatalogRuntimeProvider);
			if(runtimeProvider != null) {
				catalog.setRuntimeProvider(runtimeProvider);
				logToClient(MessageType.Info, "Runtime provider set to: " + camelCatalogRuntimeProvider);
				return updateCatalogRuntimeProviderVersion(CamelRuntimeProvider.valueOf(camelCatalogRuntimeProvider), camelCatalogRuntimeProviderVersion, catalog);
			} else {
				logToClient(MessageType.Warning, "Unknown runtime provider: " + camelCatalogRuntimeProvider);
			}
		}
		return RuntimeConfigurationProperties.NONE;
	}

	/**
	 * Loads the catalog of the requested version of the runtime, which provides the components available on this runtime
	 * and their runtime specific metadata. Otherwise the catalog of the runtime bundled with the Language Server is used.
	 * @return the configuration properties that this version of the runtime adds to the ones of Camel Main
	 */
	private RuntimeConfigurationProperties updateCatalogRuntimeProviderVersion(CamelRuntimeProvider runtimeProvider, String runtimeProviderVersion, DefaultCamelCatalog catalog) {
		if (runtimeProviderVersion == null || runtimeProviderVersion.isEmpty() || runtimeProvider.getCatalogArtifactId() == null) {
			return RuntimeConfigurationProperties.NONE;
		}
		logToClient(MessageType.Info, "Loading " + runtimeProvider + " runtime provider catalog version: " + runtimeProviderVersion);
		MavenVersionManager versionManager;
		if (catalog.getVersionManager() instanceof MavenVersionManager) {
			versionManager = (MavenVersionManager) catalog.getVersionManager();
		} else {
			versionManager = new MavenVersionManager();
			catalog.setVersionManager(versionManager);
		}
		addRedHatRepositoryIfNeeded(versionManager, runtimeProviderVersion);
		if (catalog.loadRuntimeProviderVersion(runtimeProvider.getCatalogGroupId(), runtimeProvider.getCatalogArtifactId(), runtimeProviderVersion)) {
			logToClient(MessageType.Info, "Runtime provider catalog version loaded successfully: " + runtimeProviderVersion);
			return loadRuntimeConfigurationProperties(runtimeProvider, runtimeProviderVersion, versionManager, catalog);
		} else {
			LOGGER.warn("Cannot load {} runtime provider catalog with version {}", runtimeProvider, runtimeProviderVersion);
			logToClient(MessageType.Warning, "Cannot load " + runtimeProvider + " runtime provider catalog with version " + runtimeProviderVersion);
			return RuntimeConfigurationProperties.NONE;
		}
	}

	private RuntimeConfigurationProperties loadRuntimeConfigurationProperties(CamelRuntimeProvider runtimeProvider, String runtimeProviderVersion, MavenVersionManager versionManager, DefaultCamelCatalog catalog) {
		String runtime = runtimeProvider.getTitle() + " " + runtimeProviderVersion;
		RuntimeConfigurationProperties properties = new RuntimeConfigurationPropertiesLoader(versionManager).load(runtimeProvider, runtimeProviderVersion, catalog.mainModel());
		if (properties.isEmpty()) {
			logToClient(MessageType.Info, "No configuration properties found for " + runtime);
		} else {
			logToClient(MessageType.Info, String.format("Loaded %d configuration properties of %s", properties.getProperties().size(), runtime));
		}
		return properties;
	}

	/**
	 * Registers the configuration properties that Camel JBang and the runtime add to the ones of Camel Main,
	 * for the completion, the hover and the validation of the application properties files.
	 */
	private void registerConfigurationProperties(DefaultCamelCatalog catalog, RuntimeConfigurationProperties runtimeProperties) {
		RuntimeConfigurationProperties.register(catalog, RuntimeConfigurationPropertiesLoader.loadCamelJBang(catalog).with(runtimeProperties));
	}

	private void updateCatalogVersion(String camelVersion, DefaultCamelCatalog catalog) {
		if (camelVersion != null && !camelVersion.isEmpty()) {
			logToClient(MessageType.Info, "Loading Camel catalog version: " + camelVersion);
			MavenVersionManager versionManager = new MavenVersionManager();
			addRedHatRepositoryIfNeeded(versionManager, camelVersion);
			catalog.setVersionManager(versionManager);
			if (!catalog.loadVersion(camelVersion)) {
				LOGGER.warn("Cannot load Camel catalog with version {}", camelVersion);
				logToClient(MessageType.Warning, "Cannot load Camel catalog with version " + camelVersion);
			} else {
				logToClient(MessageType.Info, "Camel catalog version loaded successfully: " + camelVersion);
			}
		} else {
			logToClient(MessageType.Info, "No Camel catalog version specified, using built-in default: " + catalog.getCatalogVersion());
		}
	}

	private void addRedHatRepositoryIfNeeded(MavenVersionManager versionManager, String version) {
		if (version.contains("redhat")) {
			versionManager.addMavenRepository("central", "https://repo1.maven.org/maven2/");
			versionManager.addMavenRepository("maven.redhat.ga", "https://maven.repository.redhat.com/ga/");
			logToClient(MessageType.Info, "Added Red Hat GA Maven repository for version: " + version);
		}
	}

	private void logToClient(MessageType type, String message) {
		if (camelLanguageServer.getClient() != null) {
			camelLanguageServer.getClient().logMessage(new MessageParams(type, message));
		}
	}

	private CompletableFuture<List<CompletionItem>> getYamlDslCompletions(TextDocumentItem textDocumentItem, Position position) {
		YamlDslLines lines = new YamlDslLines(textDocumentItem.getText());
		boolean markdown = getSettingsManager().isMardownSupport();
		CamelYamlParametersCompletionProcessor parametersProcessor = new CamelYamlParametersCompletionProcessor(lines, getCamelCatalog(), getKameletsCatalogManager(), markdown);
		if (parametersProcessor.isInParametersBlock(position)) {
			return parametersProcessor.getCompletions(position);
		}
		return new CamelYamlDslKeywordProcessor(lines, getCamelCatalog(), markdown).getCompletions(position);
	}

	private CompletableFuture<Hover> getYamlDslHover(TextDocumentItem textDocumentItem, Position position) {
		YamlDslLines lines = new YamlDslLines(textDocumentItem.getText());
		boolean markdown = getSettingsManager().isMardownSupport();
		CamelYamlParametersCompletionProcessor parametersProcessor = new CamelYamlParametersCompletionProcessor(lines, getCamelCatalog(), getKameletsCatalogManager(), markdown);
		if (parametersProcessor.isInParametersBlock(position)) {
			return parametersProcessor.getHover(position);
		}
		return new CamelYamlDslKeywordProcessor(lines, getCamelCatalog(), markdown).getHover(position);
	}

	/**
	 * @return whether the position is in a Camel YAML DSL document, outside of an endpoint URI.
	 * Endpoint URIs are handled by the processors shared by all the DSLs.
	 */
	private boolean isCamelYamlDslKey(TextDocumentItem textDocumentItem, Position position) {
		ParserFileHelperFactory parserFileHelperFactory = new ParserFileHelperFactory();
		if (!parserFileHelperFactory.isCamelYamlDSL(textDocumentItem, textDocumentItem.getUri())) {
			return false;
		}
		try {
			ParserFileHelper parserFileHelper = parserFileHelperFactory.getCorrespondingParserFileHelper(textDocumentItem, position.getLine());
			return parserFileHelper == null || parserFileHelper.getCamelComponentUri(textDocumentItem, position) == null;
		} catch (Exception e) {
			// a line which cannot be parsed is left to the endpoint URI processors, as before
			return false;
		}
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
			} else if (isCamelYamlDslKey(textDocumentItem, completionParams.getPosition())) {
				return getYamlDslCompletions(textDocumentItem, completionParams.getPosition()).thenApply(Either::forLeft);
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
			return new CamelPropertiesFileHoverProcessor(textDocumentItem, getSettingsManager().isMardownSupport()).getHover(hoverParams.getPosition(), getCamelCatalog(), getKameletsCatalogManager());
		} else if(new CamelKModelineParser().isOnCamelKModeline(hoverParams.getPosition().getLine(), textDocumentItem)) {
			return new CamelKModelineHoverProcessor(textDocumentItem).getHover(hoverParams.getPosition(), getCamelCatalog());
		} else if (textDocumentItem != null && isCamelYamlDslKey(textDocumentItem, hoverParams.getPosition())) {
			return getYamlDslHover(textDocumentItem, hoverParams.getPosition());
		} else {
			if (textDocumentItem != null && uri.endsWith(".xml")) {
				CompletableFuture<Hover> eipHover = new CamelXmlEipHoverProcessor(textDocumentItem, getCamelCatalog(), getSettingsManager().isMardownSupport()).getHover(hoverParams.getPosition());
				if (eipHover != null) {
					return eipHover;
				}
			}
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
			// We support providing folding rages only when the document are opened. It doesn't make sense anyway when it is closed
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

	public SettingsManager getSettingsManager() {
		return camelLanguageServer.getSettingsManager();
	}
	
	public KameletsCatalogManager getKameletsCatalogManager() {
		return kameletsCatalogManager;
	}
}
