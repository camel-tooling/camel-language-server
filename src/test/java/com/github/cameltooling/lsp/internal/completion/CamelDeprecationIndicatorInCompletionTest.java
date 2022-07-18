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
package com.github.cameltooling.lsp.internal.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

class CamelDeprecationIndicatorInCompletionTest extends AbstractCamelLanguageServerTest {

	@Test
	void testCamelComponentDeprecation() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(RouteTextBuilder.createXMLBlueprintRoute("acomponent:deprecated"));
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 15));
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items).hasSize(1);
		assertThat(items.get(0).getDeprecated()).isTrue();
		assertThat(items.get(0).getTags()).contains(CompletionItemTag.Deprecated);
	}
	
	@Test
	void testParameterDeprecation() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(RouteTextBuilder.createXMLBlueprintRoute("acomponent:withsyntax?aparam"));
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 39));
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items).hasSize(1);
		assertThat(items.get(0).getDeprecated()).isTrue();
		assertThat(items.get(0).getTags()).contains(CompletionItemTag.Deprecated);
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
				"  },\n" + 
				"  \"properties\": {\n" +
				"\"aparam\": { \"kind\": \"parameter\", \"displayName\": \"A Parameter deprecated\", \"group\": \"common\", \"required\": false, \"type\": \"string\", \"javaType\": \"java.lang.String\", \"deprecated\": true, \"secret\": false, \"defaultValue\": \"org.apache.camel.event\", \"configurationClass\": \"org.apache.camel.component.knative.KnativeConfiguration\", \"configurationField\": \"configuration\", \"description\": \"A parameter description\" }\n" + 
				"  }\n" + 
				"}";
		return createMapSettingsWithComponent(component);
	}
}
