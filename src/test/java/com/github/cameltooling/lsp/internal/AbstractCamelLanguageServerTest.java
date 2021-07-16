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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.junit.jupiter.api.AfterEach;

import com.github.cameltooling.lsp.internal.telemetry.TelemetryEvent;
import com.google.common.io.Files;
import com.google.gson.Gson;

public abstract class AbstractCamelLanguageServerTest {

	protected static final String KAFKA_SYNTAX_HOVER = "kafka:topic";
	protected static final String AHC_DOCUMENTATION_BEFORE_3_3 = "To call external HTTP services using Async Http Client.";
	protected static final String AHC_DOCUMENTATION = "Call external HTTP services using Async Http Client.";
	protected static final String FILE_FILTER_DOCUMENTATION = "Pluggable filter as a org.apache.camel.component.file.GenericFileFilter class. Will skip files if filter returns false in its accept() method.";
	protected static final String DUMMY_URI = "dummyUri";
	private String extensionUsed;
	protected PublishDiagnosticsParams lastPublishedDiagnostics;
	protected List<TelemetryEvent> telemetryEvents = new ArrayList<>();
	protected CamelLanguageServer camelLanguageServer;

	public AbstractCamelLanguageServerTest() {
		super();
	}
	
	@AfterEach
	public void tearDown() {
		if (camelLanguageServer != null) {
			camelLanguageServer.stopServer();
		}
		telemetryEvents.clear();
	}

	protected CompletionItem createExpectedAhcCompletionItem(int lineStart, int characterStart, int lineEnd, int characterEnd) {
		CompletionItem expectedAhcCompletioncompletionItem = new CompletionItem("ahc:httpUri");
		expectedAhcCompletioncompletionItem.setDocumentation(AHC_DOCUMENTATION);
		expectedAhcCompletioncompletionItem.setDeprecated(false);
		expectedAhcCompletioncompletionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(lineStart, characterStart), new Position(lineEnd, characterEnd)), "ahc:httpUri")));
		return expectedAhcCompletioncompletionItem;
	}
	
	protected CompletionItem createExpectedAhcCompletionItemForVersionPriorTo33(int lineStart, int characterStart, int lineEnd, int characterEnd) {
		CompletionItem expectedAhcCompletioncompletionItem = createExpectedAhcCompletionItem(lineStart, characterStart, lineEnd, characterEnd);
		expectedAhcCompletioncompletionItem.setDocumentation(AHC_DOCUMENTATION_BEFORE_3_3);
		return expectedAhcCompletioncompletionItem;
	}
	
	final class DummyLanguageClient implements LanguageClient {

		@Override
		public void telemetryEvent(Object object) {
			AbstractCamelLanguageServerTest.this.telemetryEvents.add((TelemetryEvent) object);
		}

		@Override
		public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
			return null;
		}

		@Override
		public void showMessage(MessageParams messageParams) {
		}

		@Override
		public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
			AbstractCamelLanguageServerTest.this.lastPublishedDiagnostics = diagnostics;
		}

		@Override
		public void logMessage(MessageParams message) {
		}
	}

	protected CamelLanguageServer initializeLanguageServer(String text) throws URISyntaxException, InterruptedException, ExecutionException {
		return initializeLanguageServer(text, ".xml");
	}
	
	protected CamelLanguageServer initializeLanguageServerWithFileName(String text, String filename) throws URISyntaxException, InterruptedException, ExecutionException {
		return initializeLanguageServer(filename.substring(filename.lastIndexOf('.'), filename.length()), createTestTextDocumentWithFilename(text, filename));
	}

	protected CamelLanguageServer initializeLanguageServer(String text, String suffixFileName) throws URISyntaxException, InterruptedException, ExecutionException {
		return initializeLanguageServer(suffixFileName, createTestTextDocument(text, suffixFileName));
	}

	protected CamelLanguageServer initializeLanguageServer(String suffixFileName, TextDocumentItem... documentItems) throws URISyntaxException, InterruptedException, ExecutionException {
		this.extensionUsed = suffixFileName;
		initializeLanguageServer(getInitParams());
		for (TextDocumentItem docItem : documentItems) {
			camelLanguageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(docItem));
		}
		return camelLanguageServer;
	}
	
	private void initializeLanguageServer(InitializeParams params) throws ExecutionException, InterruptedException {
		camelLanguageServer = new CamelLanguageServer();
		camelLanguageServer.connect(new DummyLanguageClient());
		camelLanguageServer.startServer();
		CompletableFuture<InitializeResult> initialize = camelLanguageServer.initialize(params);

		assertThat(initialize).isCompleted();
		assertThat(initialize.get().getCapabilities().getCompletionProvider().getResolveProvider()).isTrue();
		
		InitializedParams initialized = new InitializedParams();
		camelLanguageServer.initialized(initialized);
	}
	
	private InitializeParams getInitParams() throws URISyntaxException {
		InitializeParams params = new InitializeParams();
		params.setProcessId(new Random().nextInt());
		params.setRootUri(getTestResource("/workspace/").toURI().toString());
		params.setInitializationOptions(getInitializationOptions());
		return params;
	}
	
	protected Map<Object, Object> getInitializationOptions() {
		return Collections.emptyMap();
	}
	
	protected CamelLanguageServer initializeLanguageServer(InputStream stream, String suffixFileName) {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream))) {
            return initializeLanguageServer(buffer.lines().collect(Collectors.joining("\n")), suffixFileName);
        } catch (ExecutionException | InterruptedException | URISyntaxException | IOException ex) {
        	return null;
        }
	}
	
	protected CamelLanguageServer initializeLanguageServerWithFileName(InputStream stream, String fileName) {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream))) {
            return initializeLanguageServerWithFileName(buffer.lines().collect(Collectors.joining("\n")), fileName);
        } catch (ExecutionException | InterruptedException | URISyntaxException | IOException ex) {
        	return null;
        }
	}
	
	protected CamelLanguageServer initializeLanguageServer(File camelFile) throws URISyntaxException, InterruptedException, ExecutionException, IOException {
		return initializeLanguageServer(getExtensionByStringHandling(camelFile.getName()), new TextDocumentItem(camelFile.toURI().toString(), CamelLanguageServer.LANGUAGE_ID, 0, new String(Files.toByteArray(camelFile))));
	}
	
	private String getExtensionByStringHandling(String filename) {
		int indexOfLastDot = filename.lastIndexOf('.');
		if(indexOfLastDot != -1) {
			return filename.substring(indexOfLastDot + 1);
		} else {
			return "";
		}
	}
	
	private TextDocumentItem createTestTextDocument(String text, String suffixFileName) {
		return createTestTextDocumentWithFilename(text, DUMMY_URI + suffixFileName);
	}
	
	private TextDocumentItem createTestTextDocumentWithFilename(String text, String fileName) {
		return new TextDocumentItem(fileName, CamelLanguageServer.LANGUAGE_ID, 0, text);
	}

	protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletionFor(CamelLanguageServer camelLanguageServer, Position position) {
		return getCompletionFor(camelLanguageServer, position, DUMMY_URI+extensionUsed);
	}
	
	protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletionFor(CamelLanguageServer camelLanguageServer, Position position, String filename) {
		TextDocumentService textDocumentService = camelLanguageServer.getTextDocumentService();
		CompletionParams completionParams = new CompletionParams(new TextDocumentIdentifier(filename), position);
		return textDocumentService.completion(completionParams);
	}
	
	protected CompletableFuture<List<Either<SymbolInformation,DocumentSymbol>>> getDocumentSymbolFor(CamelLanguageServer camelLanguageServer) {
		return getDocumentSymbolFor(camelLanguageServer, DUMMY_URI+extensionUsed);
	}
	
	protected CompletableFuture<List<Either<SymbolInformation,DocumentSymbol>>> getDocumentSymbolFor(CamelLanguageServer camelLanguageServer, String uri) {
		TextDocumentService textDocumentService = camelLanguageServer.getTextDocumentService();
		DocumentSymbolParams params = new DocumentSymbolParams(new TextDocumentIdentifier(uri));
		return textDocumentService.documentSymbol(params);
	}

	protected CompletableFuture<List<? extends Location>> getReferencesFor(CamelLanguageServer camelLanguageServer, Position position) {
		return getReferencesFor(camelLanguageServer, position, DUMMY_URI+extensionUsed);
	}
	
	protected CompletableFuture<List<? extends Location>> getReferencesFor(CamelLanguageServer camelLanguageServer, Position position, String uri) {
		TextDocumentService textDocumentService = camelLanguageServer.getTextDocumentService();
		ReferenceParams params = new ReferenceParams();
		params.setPosition(position);
		params.setTextDocument(new TextDocumentIdentifier(uri));
		return textDocumentService.references(params);
	}
	
	protected CompletableFuture<Either<List<? extends Location>,List<? extends LocationLink>>> getDefinitionsFor(CamelLanguageServer camelLanguageServer, Position position) {
		TextDocumentService textDocumentService = camelLanguageServer.getTextDocumentService();
		DefinitionParams params = new DefinitionParams();
		params.setPosition(position);
		params.setTextDocument(new TextDocumentIdentifier(DUMMY_URI+extensionUsed));
		return textDocumentService.definition(params);
	}

	public File getTestResource(String name) throws URISyntaxException {
		return Paths.get(CamelLanguageServerTest.class.getResource(name).toURI()).toFile();
	}
	
	protected boolean hasTextEdit(CompletionItem item) {
		return item != null && item.getTextEdit().getLeft() != null;
	}

	protected Map<Object, Object> createMapSettingsWithComponent(String component) {
		Map<Object, Object> camelIntializationOptions = new HashMap<>();
		camelIntializationOptions.put("extra-components", Collections.singletonList(new Gson().fromJson(component, Map.class)));
		HashMap<Object, Object> initializationOptions = new HashMap<>();
		initializationOptions.put("camel", camelIntializationOptions);
		return initializationOptions;
	}
	
	protected void createFolderWithFile(String folderName, String fileName, File parent) throws IOException {
		File aSiblingFolder = new File(parent, folderName);
		aSiblingFolder.mkdir();
		File aPropertiesFileInSiblingFolder = new File(aSiblingFolder, fileName);
		aPropertiesFileInSiblingFolder.createNewFile();
	}
	
}