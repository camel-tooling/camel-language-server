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

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.DocumentLinkOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
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

/**
 * this is the actual server implementation
 * 
 * @author lhein
 */
public class CamelLanguageServer extends AbstractLanguageServer implements LanguageServer, LanguageClientAware {
	
	/** The usual Logger. */
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelLanguageServer.class);
	public static final String LANGUAGE_ID = "LANGUAGE_ID_APACHE_CAMEL";
	
	private LanguageClient client;
	
	public CamelLanguageServer() {
		super.setTextDocumentService(new CamelTextDocumentService());
		super.setWorkspaceService(new CamelWorkspaceService());
	}
	
	@Override
	public void connect(LanguageClient client) {
		this.client = client;
		sendLogMessageNotification(MessageType.Info, "Connected to Language Server...");
	}
	
	@Override
	public void exit() {
		super.stopServer();
		System.exit(0);
	}
	
	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		LOGGER.info("Initializing capabilities of the server...");
		Integer processId = params.getProcessId();
		if(processId != null) {
			setParentProcessId(processId.longValue());
		} else {
			LOGGER.error("Missing Parent process ID!!");
			setParentProcessId(0);
		}
		
		InitializeResult result = new InitializeResult();
		
		ServerCapabilities capabilities = new ServerCapabilities();
		capabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental);
		capabilities.setCompletionProvider(new CompletionOptions(Boolean.TRUE, Arrays.asList(".","?","&")));
		capabilities.setHoverProvider(Boolean.FALSE);
		capabilities.setDefinitionProvider(Boolean.FALSE);
		capabilities.setDocumentSymbolProvider(Boolean.FALSE);
		capabilities.setWorkspaceSymbolProvider(Boolean.FALSE);
		capabilities.setReferencesProvider(Boolean.FALSE);
		capabilities.setDocumentHighlightProvider(Boolean.FALSE);
		capabilities.setDocumentFormattingProvider(Boolean.FALSE);
		capabilities.setDocumentRangeFormattingProvider(Boolean.FALSE);
		capabilities.setDocumentLinkProvider(new DocumentLinkOptions(Boolean.FALSE));
		capabilities.setCodeLensProvider(new CodeLensOptions(Boolean.FALSE));
		
		result.setCapabilities(capabilities);
		return CompletableFuture.completedFuture(result);
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
	 * Sends the given <code>log message notification</code> back to the client
	 * as a notification
	 * 
	 * @param type
	 *            the type of message
	 * @param msg
	 *            The message to send back to the client
	 */
	public void sendLogMessageNotification(final MessageType type, final String msg) {
		client.logMessage(new MessageParams(type, msg));
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
		client.showMessage(new MessageParams(type, msg));
	}
}
