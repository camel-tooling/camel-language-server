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
package com.github.cameltooling.lsp.internal.completion.camelapplicationproperties;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

public class CamelPropertiesComponentCompletionTest extends AbstractCamelLanguageServerTest {

	@Test
	public void testProvideCompletion() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 16));
		
		assertThat(completions.get().getLeft()).contains(createExpectedCompletionItem());
	}
	
	@Test
	public void testProvideCompletionFilteredWithBeginningTyped() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 20));
		
		assertThat(completions.get().getLeft()).containsOnly(createExpectedCompletionItem());
	}
	
	@Test
	public void testProvideCompletionNoCompletionAtWrongPosition() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 14));
		
		assertThat(completions.get().getLeft()).isEmpty();
	}
	
	protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> retrieveCompletion(Position position) throws URISyntaxException, InterruptedException, ExecutionException {
		String fileName = "a.properties";
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(".properties", new TextDocumentItem(fileName, CamelLanguageServer.LANGUAGE_ID, 0, "camel.component.acomp"));
		return getCompletionFor(camelLanguageServer, position, fileName);
	}
	
	protected CompletionItem createExpectedCompletionItem() {
		CompletionItem expectedAhcCompletioncompletionItem = new CompletionItem("acomponent");
		expectedAhcCompletioncompletionItem.setDocumentation("Description of my component.");
		expectedAhcCompletioncompletionItem.setDeprecated(false);
		return expectedAhcCompletioncompletionItem;
	}
	
	@Override
	protected Map<Object, Object> getInitializationOptions() {
		String component = "{\n" + 
				" \"component\": {\n" + 
				"    \"kind\": \"component\",\n" + 
				"    \"scheme\": \"acomponent\",\n" + 
				"    \"syntax\": \"acomponent:withsyntax\",\n" + 
				"    \"description\": \"Description of my component.\",\n" + 
				"    \"deprecated\": false\n" + 
				"  },\n" + 
				"  \"componentProperties\": {\n" + 
				"  },\n" + 
				"  \"properties\": {\n" +
				"  }\n" + 
				"}";
		return createMapSettingsWithComponent(component);
	}
	
}
