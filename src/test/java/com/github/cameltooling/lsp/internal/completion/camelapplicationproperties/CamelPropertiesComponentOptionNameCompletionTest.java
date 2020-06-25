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
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelPropertiesComponentOptionNameCompletionTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testProvideCompletion() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.");
		
		assertThat(completions.get().getLeft()).hasSize(2);
	}
	
	@Test
	void testProvideCompletionHasDefaultValue() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.");
		
		assertThat(completions.get().getLeft().get(0).getInsertText()).isEqualTo("aComponentProperty=aDefaultValue");
	}
	
	@Test
	void testProvideCompletionWithoutDefaultValueIfAValueAlreadyProvided() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.aComponentProperty=aValue");
		CompletionItem expectedCompletionItem = new CompletionItem("aComponentProperty");
		expectedCompletionItem.setInsertText("aComponentProperty");
		expectedCompletionItem.setDocumentation("A parameter description");
		expectedCompletionItem.setDeprecated(false);
		expectedCompletionItem.setDetail(String.class.getName());
		expectedCompletionItem.setTextEdit(new TextEdit(new Range(new Position(0, 27), new Position(0, 45)), "aComponentProperty"));
		assertThat(completions.get().getLeft()).contains(expectedCompletionItem);
	}
	
	@Test
	void testInsertAndReplace() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 27), "camel.component.acomponent.awrongtoreplace=aValue");
		CompletionItem expectedCompletionItem = new CompletionItem("aComponentProperty");
		expectedCompletionItem.setInsertText("aComponentProperty");
		expectedCompletionItem.setDocumentation("A parameter description");
		expectedCompletionItem.setDeprecated(false);
		expectedCompletionItem.setDetail(String.class.getName());
		expectedCompletionItem.setTextEdit(new TextEdit(new Range(new Position(0, 27), new Position(0, 42)), "aComponentProperty"));
		assertThat(completions.get().getLeft()).contains(expectedCompletionItem);
	}
	
	@Test
	void testProvideNoCompletionAfterComponentproperty() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 46), "camel.component.acomponent.aComponentProperty.");
		
		assertThat(completions.get().getLeft()).isEmpty();
	}
	
	@Test
	void testProvideFilteredCompletionWhenInsideValue() throws Exception {
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = retrieveCompletion(new Position(0, 29), "camel.component.acomponent.aComponentProperty.");
		
		assertThat(completions.get().getLeft()).hasSize(1);
		assertThat(completions.get().getLeft().get(0).getInsertText()).isEqualTo("aComponentProperty=aDefaultValue");
	}
		
	protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> retrieveCompletion(Position position, String propertyEntry) throws URISyntaxException, InterruptedException, ExecutionException {
		String fileName = "a.properties";
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(fileName, new TextDocumentItem(fileName, CamelLanguageServer.LANGUAGE_ID, 0, propertyEntry));
		return getCompletionFor(camelLanguageServer, position, fileName);
	}

	@Override
	protected Map<Object, Object> getInitializationOptions() {
		String component = "{\n" + 
				" \"component\": {\n" + 
				"    \"kind\": \"component\",\n" + 
				"    \"scheme\": \"acomponent\",\n" + 
				"    \"syntax\": \"acomponent:withsyntax\",\n" + 
				"    \"title\": \"A Component\",\n" + 
				"    \"description\": \"Description of my component.\",\n" + 
				"    \"label\": \"\",\n" + 
				"    \"deprecated\": true,\n" + 
				"    \"deprecationNote\": \"\",\n" + 
				"    \"async\": false,\n" + 
				"    \"consumerOnly\": true,\n" + 
				"    \"producerOnly\": false,\n" + 
				"    \"lenientProperties\": false,\n" + 
				"    \"javaType\": \"org.test.AComponent\",\n" + 
				"    \"firstVersion\": \"1.0.0\",\n" + 
				"    \"groupId\": \"org.test\",\n" + 
				"    \"artifactId\": \"camel-acomponent\",\n" + 
				"    \"version\": \"3.0.0\"\n" + 
				"  },\n" + 
				"  \"componentProperties\": {\n" + 
				"\"aComponentProperty\": { \"kind\": \"parameter\", \"displayName\": \"A Component property \", \"group\": \"common\", \"required\": false, \"type\": \"string\", \"javaType\": \"java.lang.String\", \"deprecated\": false, \"secret\": false, \"defaultValue\": \"aDefaultValue\", \"configurationClass\": \"org.apache.camel.component.knative.KnativeConfiguration\", \"configurationField\": \"configuration\", \"description\": \"A parameter description\" },\n" +
				"\"aSecondComponentProperty\": { \"kind\": \"parameter\", \"displayName\": \"A Second Component property \", \"group\": \"common\", \"required\": false, \"type\": \"string\", \"javaType\": \"java.lang.String\", \"deprecated\": false, \"secret\": false, \"defaultValue\": \"aDefaultValue\", \"configurationClass\": \"org.apache.camel.component.knative.KnativeConfiguration\", \"configurationField\": \"configuration\", \"description\": \"A second parameter description\" }\n" +
				"  },\n" + 
				"  \"properties\": {\n" +
				"  }\n" + 
				"}";
		return createMapSettingsWithComponent(component);
	}
	
	
}
