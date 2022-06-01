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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileEvent;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.WorkspaceSymbol;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.settings.SettingsManager;

/**
 * @author lhein
 */
public class CamelWorkspaceService implements WorkspaceService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelWorkspaceService.class);
	private SettingsManager settingsManager;

	public CamelWorkspaceService(SettingsManager settingsManager) {
		this.settingsManager = settingsManager;
	}

	@Override
	public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params) {
		LOGGER.info("SERVER: symbolQuery: {}", params.getQuery());
		return CompletableFuture.completedFuture(Either.forRight(Collections.emptyList()));
	}

	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		Object settings = params.getSettings();
		settingsManager.apply(params);
		LOGGER.info("SERVER: changeConfig: settings -> {}", settings);
	}

	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		List<FileEvent> settings = params.getChanges();
		LOGGER.info("SERVER: changeWatchedFiles: size -> {}", settings.size());
	}
}
