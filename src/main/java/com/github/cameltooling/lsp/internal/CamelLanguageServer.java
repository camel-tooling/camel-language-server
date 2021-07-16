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

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.settings.SettingsManager;
import com.github.cameltooling.lsp.internal.telemetry.TelemetryManager;

/**
 * this is the actual server implementation
 * 
 * @author lhein
 */
public class CamelLanguageServer extends AbstractLanguageServer implements LanguageServer, LanguageClientAware {
	
	public static final String LANGUAGE_ID = "LANGUAGE_ID_APACHE_CAMEL";
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelLanguageServer.class);
	
	private LanguageClient client;
	private SettingsManager settingsManager;
	private TelemetryManager telemetryManager;
	
	public CamelLanguageServer() {
		CamelTextDocumentService textDocumentService = new CamelTextDocumentService(this);
		setTextDocumentService(textDocumentService);
		settingsManager = new SettingsManager(textDocumentService);
		setWorkspaceService(new CamelWorkspaceService(getSettingsManager()));
	}

	@Override
	public void connect(LanguageClient client) {
		this.client = client;
		telemetryManager = new TelemetryManager(client);
	}
	
	@Override
	public void exit() {
		super.stopServer();
		System.exit(0);
	}
	
	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		Integer processId = params.getProcessId();
		if(processId != null) {
			setParentProcessId(processId.longValue());
		} else {
			LOGGER.info("Missing Parent process ID!!");
			setParentProcessId(0);
		}
		
		getSettingsManager().apply(params);
		
		ServerCapabilities capabilities = createServerCapabilities();
		InitializeResult result = new InitializeResult(capabilities);
		return CompletableFuture.completedFuture(result);
	}
	
	@Override
	public void initialized(InitializedParams params) {
		LanguageServer.super.initialized(params);
		if(telemetryManager != null) {
			telemetryManager.onInitialized();
		}
	}

	private ServerCapabilities createServerCapabilities() {
		ServerCapabilities capabilities = new ServerCapabilities();
		capabilities.setTextDocumentSync(TextDocumentSyncKind.Full);
		capabilities.setCompletionProvider(new CompletionOptions(Boolean.TRUE, Arrays.asList(".","?","&", "\"", "=")));
		capabilities.setHoverProvider(Boolean.TRUE);
		capabilities.setDocumentSymbolProvider(Boolean.TRUE);
		capabilities.setReferencesProvider(Boolean.TRUE);
		capabilities.setDefinitionProvider(Boolean.TRUE);
		capabilities.setCodeActionProvider(new CodeActionOptions(Arrays.asList(CodeActionKind.QuickFix)));
		return capabilities;
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		super.shutdownServer();
		return CompletableFuture.completedFuture(new Object());
	}
	
	@Override
	public WorkspaceService getWorkspaceService() {
		return super.getWorkspaceService();
	}
	
	/**
	 * Sends the given <code>show message notification</code> back to the client
	 * as a notification
	 * 
	 * @param type
	 *            the type of message
	 * @param msg
	 *            The message to send back to the client
	 */
	public void sendShowMessageNotification(final MessageType type, final String msg) {
		getClient().showMessage(new MessageParams(type, msg));
	}

	public LanguageClient getClient() {
		return client;
	}

	public SettingsManager getSettingsManager() {
		return settingsManager;
	}
	
}
