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

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

class CamelComponentOptionsCompletionsTest extends AbstractCamelLanguageServerTest {
	
    @Test
	void testProvideCamelOptions() throws Exception {
		testProvideCamelOptions("<to uri=\"acomponent:withsyntax?\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 0, 31, getParamConsumerAndProducerCompletionItem(31,31));
	}
    
    @Test
	void testProvideCamelOptionsForConsumerOnly() throws Exception {
    	CompletionItem completionItem = new CompletionItem("paramConsumerOnly");
    	completionItem.setInsertText("paramConsumerOnly=1000");
    	completionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(0, 33), new Position(0, 33)), "paramConsumerOnly=1000")));
    	completionItem.setDocumentation("Description of parameter consumer only");
    	completionItem.setDetail("long");
    	completionItem.setDeprecated(false);
    	completionItem.setKind(CompletionItemKind.Property);
		testProvideCamelOptions(RouteTextBuilder.createXMLBlueprintRoute("acomponent:withsyntax?"), 0, 33, completionItem);
	}
    
    @Test
	void testProvideCamelOptionsForConsumerOnlyForJava() throws Exception {
    	CompletionItem completionItem = new CompletionItem("paramConsumerOnly");
    	completionItem.setInsertText("paramConsumerOnly=1000");
    	completionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(0, 28), new Position(0, 28)), "paramConsumerOnly=1000")));
    	completionItem.setDocumentation("Description of parameter consumer only");
    	completionItem.setDetail("long");
    	completionItem.setDeprecated(false);
    	completionItem.setKind(CompletionItemKind.Property);
		testProvideCamelOptions("from(\"acomponent:withsyntax?\")//camel", 0, 28, completionItem, ".java");
	}

	@Test
	void testProvideCamelOptionsForConsumerOrProducer() throws Exception {
    	CompletionItem completionItem = new CompletionItem("paramConsumerAndProducer");
    	completionItem.setInsertText("paramConsumerAndProducer=1000");
    	completionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(0, 33), new Position(0, 33)), "paramConsumerAndProducer=1000")));
    	completionItem.setDocumentation("Description of parameter both consumer and producer");
    	completionItem.setDetail("long");
    	completionItem.setDeprecated(false);
    	completionItem.setKind(CompletionItemKind.Property);
		testProvideCamelOptions(RouteTextBuilder.createXMLBlueprintRoute("acomponent:withsyntax?"), 0, 33, completionItem);
	}
    
    @Test
   	void testProvideCamelOptionsWhenAlreadyContainOptions() throws Exception {
    	testProvideCamelOptions("<to uri=\"acomponent:withsyntax?anOption=aValue&amp;\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 0, 51, getParamConsumerAndProducerCompletionItem(51,51));
   	}
    
    private void testProvideCamelOptions(String textTotest, int line, int character, CompletionItem completionItemExpected) throws URISyntaxException, InterruptedException, ExecutionException {
    	testProvideCamelOptions(textTotest, line, character, completionItemExpected, ".xml");
    }
       
    private void testProvideCamelOptions(String textTotest, int line, int character, CompletionItem completionItemExpected, String fileType) throws URISyntaxException, InterruptedException, ExecutionException {
    	CamelLanguageServer camelLanguageServer = initializeLanguageServer(textTotest, fileType);
    	
    	CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(line, character));
    	
    	assertThat(completions.get().getLeft()).contains(completionItemExpected);
    }

	private CompletionItem getParamConsumerAndProducerCompletionItem(int startCharacter, int endCharacter) {
		CompletionItem completionItem = new CompletionItem("paramConsumerAndProducer");
		completionItem.setInsertText("paramConsumerAndProducer=1000");
    	completionItem.setTextEdit(Either.forLeft(new TextEdit(new Range(new Position(0, startCharacter), new Position(0, endCharacter)), "paramConsumerAndProducer=1000")));
    	completionItem.setDocumentation("Description of parameter both consumer and producer");
    	completionItem.setDetail("long");
    	completionItem.setKind(CompletionItemKind.Property);
    	completionItem.setDeprecated(false);
		return completionItem;
	}
    
	@Override
	protected Map<Object, Object> getInitializationOptions() {
		String component = "{\n"
				+ "	\"component\": {\n"
				+ "		\"kind\": \"component\",\n"
				+ "		\"scheme\": \"acomponent\",\n"
				+ "		\"syntax\": \"acomponent:withsyntax\",\n"
				+ "		\"title\": \"A Component\",\n"
				+ "		\"description\": \"Description of my component.\",\n"
				+ "		\"label\": \"\",\n"
				+ "		\"deprecated\": false,\n"
				+ "		\"deprecationNote\": \"\",\n"
				+ "		\"async\": false,\n"
				+ "		\"consumerOnly\": false,\n"
				+ "		\"producerOnly\": false,\n"
				+ "		\"lenientProperties\": false,\n"
				+ "		\"javaType\": \"org.test.AComponent\",\n"
				+ "		\"firstVersion\": \"1.0.0\",\n"
				+ "		\"groupId\": \"org.test\",\n"
				+ "		\"artifactId\": \"camel-acomponent\",\n"
				+ "		\"version\": \"3.0.0\"\n"
				+ "	},\n"
				+ "	\"componentProperties\": {},\n"
				+ "	\"properties\": {\n"
				+ "		\"paramConsumerOnly\": {\n"
				+ "			\"kind\": \"parameter\",\n"
				+ "			\"displayName\": \"paramConsumerOnly\",\n"
				+ "			\"group\": \"consumer\",\n"
				+ "			\"label\": \"\",\n"
				+ "			\"required\": false,\n"
				+ "			\"type\": \"duration\",\n"
				+ "			\"javaType\": \"long\",\n"
				+ "			\"deprecated\": false,\n"
				+ "			\"autowired\": false,\n"
				+ "			\"secret\": false,\n"
				+ "			\"defaultValue\": \"1000\",\n"
				+ "			\"description\": \"Description of parameter consumer only\"\n"
				+ "		},\n"
				+ "		\"paramConsumerAndProducer\": {\n"
				+ "			\"kind\": \"parameter\",\n"
				+ "			\"displayName\": \"paramConsumerAndProducer\",\n"
				+ "			\"group\": \"\",\n"
				+ "			\"label\": \"\",\n"
				+ "			\"required\": false,\n"
				+ "			\"type\": \"duration\",\n"
				+ "			\"javaType\": \"long\",\n"
				+ "			\"deprecated\": false,\n"
				+ "			\"autowired\": false,\n"
				+ "			\"secret\": false,\n"
				+ "			\"defaultValue\": \"1000\",\n"
				+ "			\"description\": \"Description of parameter both consumer and producer\"\n"
				+ "		},\n"
				+ "		\"paramProducerOnly\": {\n"
				+ "			\"kind\": \"parameter\",\n"
				+ "			\"displayName\": \"paramProducerOnly\",\n"
				+ "			\"group\": \"producer\",\n"
				+ "			\"label\": \"\",\n"
				+ "			\"required\": false,\n"
				+ "			\"type\": \"duration\",\n"
				+ "			\"javaType\": \"long\",\n"
				+ "			\"deprecated\": false,\n"
				+ "			\"autowired\": false,\n"
				+ "			\"secret\": false,\n"
				+ "			\"defaultValue\": \"1000\",\n"
				+ "			\"description\": \"Description of parameter producer only\"\n"
				+ "		}\n"
				+ "	}\n"
				+ "}";
		return createMapSettingsWithComponent(component);
	}
	
}
