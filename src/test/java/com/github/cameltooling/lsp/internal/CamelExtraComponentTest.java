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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

class CamelExtraComponentTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testAddExtraComponentToCatalog() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n");
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 11));
		assertThat(completions.get().getLeft()).contains(createBasicExpectedCompletionItem());
	}

	@Test
	void testUpdateOfConfig() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n");
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 11));
		assertThat(completions.get().getLeft()).contains(createBasicExpectedCompletionItem());
		
		
		String component = "{\n" + 
				" \"component\": {\n" + 
				"    \"kind\": \"component\",\n" + 
				"    \"scheme\": \"aSecondcomponent\",\n" + 
				"    \"syntax\": \"aSecondcomponent:withsyntax\",\n" + 
				"    \"title\": \"A Second Component\",\n" + 
				"    \"description\": \"Description of my second component.\",\n" + 
				"    \"label\": \"\",\n" + 
				"    \"deprecated\": false,\n" + 
				"    \"deprecationNote\": \"\",\n" + 
				"    \"async\": false,\n" + 
				"    \"consumerOnly\": true,\n" + 
				"    \"producerOnly\": false,\n" + 
				"    \"lenientProperties\": false,\n" + 
				"    \"javaType\": \"org.test.ASecondComponent\",\n" + 
				"    \"firstVersion\": \"1.0.0\",\n" + 
				"    \"groupId\": \"org.test\",\n" + 
				"    \"artifactId\": \"camel-asecondcomponent\",\n" + 
				"    \"version\": \"3.0.0-RC3\"\n" + 
				"  },\n" + 
				"  \"componentProperties\": {\n" + 
				"  },\n" + 
				"  \"properties\": {\n" + 
				"  }\n" + 
				"}";
		DidChangeConfigurationParams params = new DidChangeConfigurationParams(createMapSettingsWithComponent(component));
		camelLanguageServer.getWorkspaceService().didChangeConfiguration(params);
		
		assertThat(getCompletionFor(camelLanguageServer, new Position(0, 11)).get().getLeft())
		.contains(createExpectedExtraComponentCompletionItem(0, 11, 0, 11, "aSecondcomponent:withsyntax", "Description of my second component."));
	}
	
	private CompletionItem createExpectedExtraComponentCompletionItem(int lineStart, int characterStart, int lineEnd, int characterEnd, String syntax, String description) {
		CompletionItem expectedAhcCompletioncompletionItem = new CompletionItem(syntax);
		expectedAhcCompletioncompletionItem.setDocumentation(description);
		expectedAhcCompletioncompletionItem.setDeprecated(false);
		expectedAhcCompletioncompletionItem.setTextEdit(new TextEdit(new Range(new Position(lineStart, characterStart), new Position(lineEnd, characterEnd)), syntax));
		return expectedAhcCompletioncompletionItem;
	}
	
	private CompletionItem createBasicExpectedCompletionItem() {
		return createExpectedExtraComponentCompletionItem(0, 11, 0, 11, "acomponent:withsyntax", "Description of my component.");
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
				"    \"deprecated\": false,\n" + 
				"    \"deprecationNote\": \"\",\n" + 
				"    \"async\": false,\n" + 
				"    \"consumerOnly\": true,\n" + 
				"    \"producerOnly\": false,\n" + 
				"    \"lenientProperties\": false,\n" + 
				"    \"javaType\": \"org.test.AComponent\",\n" + 
				"    \"firstVersion\": \"1.0.0\",\n" + 
				"    \"groupId\": \"org.test\",\n" + 
				"    \"artifactId\": \"camel-acomponent\",\n" + 
				"    \"version\": \"3.0.0-RC3\"\n" + 
				"  },\n" + 
				"  \"componentProperties\": {\n" + 
				"  },\n" + 
				"  \"properties\": {\n" + 
				"  }\n" + 
				"}";
		return createMapSettingsWithComponent(component);
	}
}
