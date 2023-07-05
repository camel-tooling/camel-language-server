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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;


public class CamelCompletionForApisTest extends AbstractCamelLanguageServerTest {
	
	private static final Comparator<CompletionItem> COMPLETIONITEM_COMPARATOR =
			Comparator.comparing(CompletionItem::getSortText, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(Comparator.comparing(CompletionItem::getLabel));
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
			+ " \"properties\": {"
			+ "		\"aaaProperty\": {\n"
			+ "			\"kind\": \"parameter\",\n"
			+ "			\"displayName\": \"aaa property with alphabethical order before other API properties\",\n"
			+ "			\"group\": \"common\",\n"
			+ "			\"label\": \"\",\n"
			+ "			\"required\": false,\n"
			+ "			\"type\": \"string\",\n"
			+ "			\"javaType\": \"java.lang.String\",\n"
			+ "			\"deprecated\": false,\n"
			+ "			\"secret\": false,\n"
			+ "			\"description\": \"A property description\"\n"
			+ "		}"
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
		String camelUri = "aComponentWithApis:account/fetch?";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		int endOfCamelUriInText = RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length();
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, endOfCamelUriInText)).get().getLeft();
		assertThat(completions).hasSize(2);
		completions.sort(COMPLETIONITEM_COMPARATOR);
		CompletionItem completionItemForPropertyFetcher = completions.get(0);
		assertThat(completionItemForPropertyFetcher.getLabel()).isEqualTo("aPropertyFetcher");
		assertThat(completionItemForPropertyFetcher.getKind()).isEqualTo(CompletionItemKind.Variable);
		assertThat(completionItemForPropertyFetcher.getTextEdit().getLeft().getRange()).isEqualTo(new Range(new Position(0, endOfCamelUriInText), new Position(0, endOfCamelUriInText)));
	}
	
	@Test
	void testTwilio() throws Exception {
		String camelUri = "twilio:address/create?cit";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length())).get().getLeft();
		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getLabel()).isEqualTo("city");
	}
	
	@Test
	void testUpdater() throws Exception {
		String camelUri = "aComponentWithApis:account/update?";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length())).get().getLeft();
		assertThat(completions).hasSize(2);
		completions.sort(COMPLETIONITEM_COMPARATOR);
		assertThat(completions.get(0).getLabel()).isEqualTo("aPropertyUpdater");
	}
	
	private Stream<CompletionItem> filterApiBasedOptions(List<CompletionItem> completions) {
		return completions.stream().filter(completion -> CompletionItemKind.Variable.equals(completion.getKind()));
	}
	
	@Test
	void testApiName() throws Exception {
		String camelUri = "aComponentWithApis:a";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		int endOfCamelUriInText = RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length();
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, endOfCamelUriInText)).get().getLeft();
		assertThat(completions).hasSize(1);
		CompletionItem accountCompletionItem = completions.get(0);
		assertThat(accountCompletionItem.getLabel()).isEqualTo("account");
		assertThat(accountCompletionItem.getInsertText()).isEqualTo("account");
		assertThat(accountCompletionItem.getTextEdit().getLeft().getNewText()).isEqualTo("account");
		Range expectedRange = new Range(new Position(0,  endOfCamelUriInText -1), new Position(0, endOfCamelUriInText));
		assertThat(accountCompletionItem.getTextEdit().getLeft().getRange()).isEqualTo(expectedRange);
	}
	
	@Test
	void testWithPartialMethodName() throws Exception {
		String camelUri = "aComponentWithApis:account/f";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		int endOfCamelUriInText = RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length();
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, endOfCamelUriInText)).get().getLeft();
		assertThat(completions).hasSize(1);
		CompletionItem accountCompletionItem = completions.get(0);
		assertThat(accountCompletionItem.getLabel()).isEqualTo("fetch");
		assertThat(accountCompletionItem.getInsertText()).isEqualTo("fetch");
		assertThat(accountCompletionItem.getTextEdit().getLeft().getNewText()).isEqualTo("fetch");
		Range expectedRange = new Range(new Position(0,  endOfCamelUriInText -1), new Position(0, endOfCamelUriInText));
		assertThat(accountCompletionItem.getTextEdit().getLeft().getRange()).isEqualTo(expectedRange);
	}
	
	@Test
	void testMethodName() throws Exception {
		String camelUri = "aComponentWithApis:account/";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		int endOfCamelUriInText = RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length();
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, endOfCamelUriInText)).get().getLeft();
		assertThat(completions).hasSize(2);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource
	void testBasicEmpty(String testName, String camelUri) throws Exception {
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		int endOfCamelUriInText = RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length();
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, endOfCamelUriInText)).get().getLeft();
		assertThat(filterApiBasedOptions(completions)).isEmpty();
	}

	private static Stream<Arguments> testBasicEmpty() {
		return Stream.of(Arguments.of("Empty method", "aComponentWithApis:account/create?"),
				Arguments.of("Empty with missing method name", "aComponentWithApis:account?"),
				Arguments.of("Empty with invalid method name", "aComponentWithApis:account/invalid?"),
				Arguments.of("Empty with missing API name", "aComponentWithApis?"));
	}
		
	@Test
	void testNoCompletionWithNonApiBasedComponent() throws Exception {
		String camelUri = "avro:transport:host:port/messageName";
		String text = RouteTextBuilder.createXMLSpringRoute(camelUri);
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".xml");
		int endOfCamelUriInText = RouteTextBuilder.XML_PREFIX_FROM.length() + camelUri.length();
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, endOfCamelUriInText)).get().getLeft();
		assertThat(completions).isEmpty();
	}
	
	
	@Override
	protected Map<Object, Object> getInitializationOptions() {
		return createMapSettingsWithComponent(SIMPLIFIED_JSON);
	}

}
