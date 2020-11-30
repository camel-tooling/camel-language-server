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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;


public class CamelCompletionForApisTest extends AbstractCamelLanguageServerTest {
	
	public static final String SIMPLIFIED_JSON = "{\n"
			+ "	\"component\": {\n"
			+ "		\"kind\": \"component\",\n"
			+ "		\"name\": \"aComponentWithApis\",\n"
			+ "		\"title\": \"aComponentWithApis\",\n"
			+ "		\"description\": \"Interact with Twilio REST APIs using Twilio Java SDK.\",\n"
			+ "		\"deprecated\": false,\n"
			+ "		\"firstVersion\": \"2.20.0\",\n"
			+ "		\"label\": \"api,messaging,cloud\",\n"
			+ "		\"javaType\": \"org.apache.camel.component.twilio.TwilioComponent\",\n"
			+ "		\"supportLevel\": \"Stable\",\n"
			+ "		\"groupId\": \"org.apache.camel\",\n"
			+ "		\"artifactId\": \"camel-twilio\",\n"
			+ "		\"version\": \"3.7.0-SNAPSHOT\",\n"
			+ "		\"scheme\": \"aComponentWithApis\",\n"
			+ "		\"extendsScheme\": \"\",\n"
			+ "		\"syntax\": \"aComponentWithApis:apiName\\/methodName\",\n"
			+ "		\"async\": false,\n"
			+ "		\"api\": true,\n"
			+ "		\"apiSyntax\": \"apiName\\/methodName\",\n"
			+ "		\"consumerOnly\": false,\n"
			+ "		\"producerOnly\": false,\n"
			+ "		\"lenientProperties\": false\n"
			+ "	},\n"
			+ "	\"apis\": {\n"
			+ "		\"account\": {\n"
			+ "			\"consumerOnly\": false,\n"
			+ "			\"producerOnly\": false,\n"
			+ "			\"description\": \"\",\n"
			+ "			\"aliases\": [\n"
			+ "				\"^creator$=create\",\n"
			+ "				\"^deleter$=delete\",\n"
			+ "				\"^fetcher$=fetch\",\n"
			+ "				\"^reader$=read\",\n"
			+ "				\"^updater$=update\"\n"
			+ "			],\n"
			+ "			\"methods\": {\n"
			+ "				\"fetcher\": {\n"
			+ "					\"description\": \"Create a AccountFetcher to execute fetch\",\n"
			+ "					\"signatures\": [\n"
			+ "						\"com.twilio.rest.api.v2010.AccountFetcher fetcher()\",\n"
			+ "						\"com.twilio.rest.api.v2010.AccountFetcher fetcher(String pathSid)\"\n"
			+ "					]\n"
			+ "				},\n"
			+ "				\"updater\": {\n"
			+ "					\"description\": \"Create a AccountUpdater to execute update\",\n"
			+ "					\"signatures\": [\n"
			+ "						\"com.twilio.rest.api.v2010.AccountUpdater updater()\",\n"
			+ "						\"com.twilio.rest.api.v2010.AccountUpdater updater(String pathSid)\"\n"
			+ "					]\n"
			+ "				}\n"
			+ "			}\n"
			+ "		}\n"
			+ "	},\n"
			+ "	\"apiProperties\": {\n"
			+ "		\"account\": {\n"
			+ "			\"methods\": {\n"
			+ "				\"fetcher\": {\n"
			+ "					\"properties\": {\n"
			+ "						\"aPropertyFetcher\": {\n"
			+ "							\"kind\": \"parameter\",\n"
			+ "							\"displayName\": \"a Property Fetcher\",\n"
			+ "							\"group\": \"common\",\n"
			+ "							\"label\": \"\",\n"
			+ "							\"required\": false,\n"
			+ "							\"type\": \"string\",\n"
			+ "							\"javaType\": \"java.lang.String\",\n"
			+ "							\"deprecated\": false,\n"
			+ "							\"secret\": false,\n"
			+ "							\"description\": \"A Property Fetcher description\",\n"
			+ "							\"optional\": false\n"
			+ "						}\n"
			+ "					}\n"
			+ "				},\n"
			+ "				\"updater\": {\n"
			+ "					\"properties\": {\n"
			+ "						\"aPropertyUpdater\": {\n"
			+ "							\"kind\": \"parameter\",\n"
			+ "							\"displayName\": \"a Property Updater\",\n"
			+ "							\"group\": \"common\",\n"
			+ "							\"label\": \"\",\n"
			+ "							\"required\": false,\n"
			+ "							\"type\": \"string\",\n"
			+ "							\"javaType\": \"java.lang.String\",\n"
			+ "							\"deprecated\": false,\n"
			+ "							\"secret\": false,\n"
			+ "							\"description\": \"Update by unique Account Sid\",\n"
			+ "							\"optional\": false\n"
			+ "						}\n"
			+ "					}\n"
			+ "				}\n"
			+ "			}\n"
			+ "		}\n"
			+ "	}\n"
			+ "}";
	
	@Test
	void testFetcher() throws Exception {
		String text = "camel.sink.url=aComponentWithApis:account/fetch?";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).hasSize(1);
		CompletionItem completionItemForPropertyFetcher = completions.get(0);
		assertThat(completionItemForPropertyFetcher.getLabel()).isEqualTo("aPropertyFetcher");
		assertThat(completionItemForPropertyFetcher.getKind()).isEqualTo(CompletionItemKind.Variable);
		assertThat(completionItemForPropertyFetcher.getTextEdit().getRange()).isEqualTo(new Range(new Position(0, text.length()), new Position(0, text.length())));
	}
	
	@Test
	void testTwilio() throws Exception {
		String text = "camel.sink.url=twilio:address/create?cit";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getLabel()).isEqualTo("city");
	}
	
	@Test
	void testUpdater() throws Exception {
		String text = "camel.sink.url=aComponentWithApis:account/update?";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getLabel()).isEqualTo("aPropertyUpdater");
	}
	
	@Test
	void testEmptyMethod() throws Exception {
		String text = "camel.sink.url=aComponentWithApis:account/create?";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).isEmpty();
	}
	
	@Test
	void testApiName() throws Exception {
		String text = "camel.sink.url=aComponentWithApis:a";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).hasSize(1);
		CompletionItem accountCompletionItem = completions.get(0);
		assertThat(accountCompletionItem.getLabel()).isEqualTo("account");
		assertThat(accountCompletionItem.getInsertText()).isEqualTo("account");
		assertThat(accountCompletionItem.getTextEdit().getNewText()).isEqualTo("account");
		Range expectedRange = new Range(new Position(0,  text.length() -1), new Position(0, text.length()));
		assertThat(accountCompletionItem.getTextEdit().getRange()).isEqualTo(expectedRange);
	}
	
	@Test
	void testWithPartialMethodName() throws Exception {
		String text = "camel.sink.url=aComponentWithApis:account/f";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).hasSize(1);
		CompletionItem accountCompletionItem = completions.get(0);
		assertThat(accountCompletionItem.getLabel()).isEqualTo("fetch");
		assertThat(accountCompletionItem.getInsertText()).isEqualTo("fetch");
		assertThat(accountCompletionItem.getTextEdit().getNewText()).isEqualTo("fetch");
		Range expectedRange = new Range(new Position(0,  text.length() -1), new Position(0, text.length()));
		assertThat(accountCompletionItem.getTextEdit().getRange()).isEqualTo(expectedRange);
	}
	
	@Test
	void testMethodName() throws Exception {
		String text = "camel.sink.url=aComponentWithApis:account/";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).hasSize(2);
	}
	
	@Test
	void testNoCompletionWithNonApiBasedComponent() throws Exception {
		String text = "camel.sink.url=avro:transport:host:port/messageName";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).isEmpty();
	}
	
	@Test
	void testEmptyWithMissingMethodName() throws Exception {
		String text = "camel.sink.url=aComponentWithApis:account?";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).isEmpty();
	}
	
	@Test
	void testEmptyWithInvalidMethodName() throws Exception {
		String text = "camel.sink.url=aComponentWithApis:account/invalid?";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).isEmpty();
	}
	
	@Test
	void testEmptyWithMissingApiName() throws Exception {
		String text = "camel.sink.url=aComponentWithApis?";
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".properties");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, text.length())).get().getLeft();
		assertThat(completions).isEmpty();
	}
	
	@Override
	protected Map<Object, Object> getInitializationOptions() {
		return createMapSettingsWithComponent(SIMPLIFIED_JSON);
	}

}
