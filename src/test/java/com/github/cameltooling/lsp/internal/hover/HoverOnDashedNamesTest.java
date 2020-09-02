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
package com.github.cameltooling.lsp.internal.hover;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class HoverOnDashedNamesTest extends AbstractCamelLanguageServerTest {
	
	@Test
	@Disabled("Disabled until basic hover is implemented FUSETOOLS2-621")
	void testHoverOnCamelComponentOptions() throws Exception {
		String propertyEntry = "camel.component.acomponent.a-component-property=test";
		String fileName = "a.properties";
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(fileName, new TextDocumentItem(fileName, CamelLanguageServer.LANGUAGE_ID, 0, propertyEntry));
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(fileName), new Position(0, 30));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo("A parameter description");
	}
	
	@Test
	@Disabled("Disabled until basic hover is implemented FUSETOOLS2-620")
	void testHoverOnCamelMainOptions() throws Exception {
		String propertyEntry = "camel.main.backlog-tracing=true";
		String fileName = "a.properties";
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(fileName, new TextDocumentItem(fileName, CamelLanguageServer.LANGUAGE_ID, 0, propertyEntry));
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(fileName), new Position(0, 13));
		CompletableFuture<Hover> hover = camelLanguageServer.getTextDocumentService().hover(hoverParams);
		
		assertThat(hover.get().getContents().getLeft().get(0).getLeft()).isEqualTo("Sets whether backlog tracing is enabled or not. Default is false.");
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
