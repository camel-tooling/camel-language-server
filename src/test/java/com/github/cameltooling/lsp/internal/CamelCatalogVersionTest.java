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

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

class CamelCatalogVersionTest extends AbstractCamelLanguageServerTest {

	private String camelCatalogVersion;

	@Test
	void testCompletionWithAnotherCatalog2xVersionLoaded() throws Exception {
		camelCatalogVersion = "2.23.4";
		
		CamelLanguageServer camelLanguageServer = basicCompletionCheckBefore3_3();
		
		checkLoadedCamelCatalogVersion(camelLanguageServer, camelCatalogVersion);
	}
	
	@Test
	void testCompletionWithCatalog3xVersionLoaded() throws Exception {
		camelCatalogVersion = "3.0.0";
		
		CamelLanguageServer camelLanguageServer = basicCompletionCheckBefore3_3();
		
		checkLoadedCamelCatalogVersion(camelLanguageServer, camelCatalogVersion);
	}
	
	@Test
	void testUpdateOfConfig() throws Exception {
		camelCatalogVersion = "3.0.0";
		
		CamelLanguageServer camelLanguageServer = basicCompletionCheckBefore3_3();
		
		assertThat(camelLanguageServer.getTextDocumentService().getCamelCatalog().get().getLoadedVersion()).isEqualTo(camelCatalogVersion);
		
		DidChangeConfigurationParams params = new DidChangeConfigurationParams(createMapSettingsWithVersion("2.23.4"));
		camelLanguageServer.getWorkspaceService().didChangeConfiguration(params);
		
		checkLoadedCamelCatalogVersion(camelLanguageServer, "2.23.4");
	}
	
	@Test
	void testCompletionFallbackWithInvalidVersion() throws Exception {
		camelCatalogVersion = "invalid";
		
		basicCompletionCheck();
	}
	
	@Test
	void testCompletionFallbackWithNullVersion() throws Exception {
		camelCatalogVersion = null;
		
		basicCompletionCheck();
	}
	
	@Test
	void testProductizedCatalog2x() throws Exception {
		camelCatalogVersion = "2.23.2.fuse-7_11_0-00037-redhat-00001";
		
		CamelLanguageServer camelLanguageServer = basicCompletionCheckBefore3_3();
		
		checkLoadedCamelCatalogVersion(camelLanguageServer, camelCatalogVersion);
	}
	
	@Test
	void testProductizedCatalog3x() throws Exception {
		camelCatalogVersion = "3.11.5.fuse-800012-redhat-00004";
		
		CamelLanguageServer camelLanguageServer = basicCompletionCheckFor3_11_5();
		
		checkLoadedCamelCatalogVersion(camelLanguageServer, camelCatalogVersion);
	}

	private CamelLanguageServer basicCompletionCheckFor3_11_5() throws URISyntaxException, InterruptedException, ExecutionException {
		return basicCompletionCheck(createExpectedAhcCompletionItemForVersion3_11_5(0, 11, 0, 11));
	}

	private CamelLanguageServer basicCompletionCheck() throws URISyntaxException, InterruptedException, ExecutionException {
		return basicCompletionCheck(createExpectedTimerCompletionItem(0, 11, 0, 11));
	}

	private CamelLanguageServer basicCompletionCheckBefore3_3() throws URISyntaxException, InterruptedException, ExecutionException {
		return basicCompletionCheck(createExpectedAhcCompletionItemForVersionPriorTo33(0, 11, 0, 11));
	}
	
	private CamelLanguageServer basicCompletionCheck(CompletionItem expectedCompletionItem) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(RouteTextBuilder.createXMLBlueprintRoute(""));
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 11));
		assertThat(completions.get().getLeft()).contains(expectedCompletionItem);
		return camelLanguageServer;
	}
	
	@Override
	protected Map<Object, Object> getInitializationOptions() {
		return createMapSettingsWithVersion(camelCatalogVersion);
	}

	private Map<Object, Object> createMapSettingsWithVersion(String camelCatalogVersion) {
		Map<Object, Object> camelIntializationOptions = new HashMap<>();
		camelIntializationOptions.put("Camel catalog version", camelCatalogVersion);
		Map<Object, Object> initializationOptions = new HashMap<>();
		initializationOptions.put("camel", camelIntializationOptions);
		return initializationOptions;
	}
	
	private void checkLoadedCamelCatalogVersion(CamelLanguageServer camelLanguageServer, String camelCatalogVersion)
			throws InterruptedException, ExecutionException {
		assertThat(camelLanguageServer.getTextDocumentService().getCamelCatalog().get().getLoadedVersion()).isEqualTo(camelCatalogVersion);
	}
	
}
