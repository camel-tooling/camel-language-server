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

class CamelComponentOptionsCompletionsTest extends AbstractCamelLanguageServerTest {
	
    @Test
	void testProvideCamelOptions() throws Exception {
		testProvideCamelOptions("<to uri=\"ahc:httpUri?\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 0, 21, getBridgeEndpointExpectedCompletionItem(21,21));
	}
    
    @Test
	void testProvideCamelOptionsForConsumerOnly() throws Exception {
    	CompletionItem completionItem = new CompletionItem("bridgeErrorHandler");
    	completionItem.setInsertText("bridgeErrorHandler=false");
    	completionItem.setTextEdit(new TextEdit(new Range(new Position(0, 27), new Position(0, 27)), "bridgeErrorHandler=false"));
    	completionItem.setDocumentation("Allows for bridging the consumer to the Camel routing Error Handler, which mean any exceptions occurred while the consumer is trying to pickup incoming messages, or the likes, will now be processed as a message and handled by the routing Error Handler. By default the consumer will use the org.apache.camel.spi.ExceptionHandler to deal with exceptions, that will be logged at WARN or ERROR level and ignored.");
    	completionItem.setDetail("boolean");
    	completionItem.setDeprecated(false);
    	completionItem.setKind(CompletionItemKind.Property);
		testProvideCamelOptions("<from uri=\"timer:timerName?\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 27, completionItem);
	}
    
    @Test
	void testProvideCamelOptionsForConsumerOnlyForJava() throws Exception {
    	CompletionItem completionItem = new CompletionItem("bridgeErrorHandler");
    	completionItem.setInsertText("bridgeErrorHandler=false");
    	completionItem.setTextEdit(new TextEdit(new Range(new Position(0, 22), new Position(0, 22)), "bridgeErrorHandler=false"));
    	completionItem.setDocumentation("Allows for bridging the consumer to the Camel routing Error Handler, which mean any exceptions occurred while the consumer is trying to pickup incoming messages, or the likes, will now be processed as a message and handled by the routing Error Handler. By default the consumer will use the org.apache.camel.spi.ExceptionHandler to deal with exceptions, that will be logged at WARN or ERROR level and ignored.");
    	completionItem.setDetail("boolean");
    	completionItem.setDeprecated(false);
    	completionItem.setKind(CompletionItemKind.Property);
		testProvideCamelOptions("from(\"timer:timerName?\")//camel", 0, 22, completionItem, ".java");
	}

	@Test
	void testProvideCamelOptionsForConsumerOrProducer() throws Exception {
    	CompletionItem completionItem = new CompletionItem("clientConfigOptions");
    	completionItem.setInsertText("clientConfigOptions=");
    	completionItem.setTextEdit(new TextEdit(new Range(new Position(0, 23), new Position(0, 23)), "clientConfigOptions="));
    	completionItem.setDocumentation("To configure the AsyncHttpClientConfig using the key/values from the Map.");
    	completionItem.setDetail("java.util.Map<java.lang.String, java.lang.Object>");
    	completionItem.setDeprecated(false);
    	completionItem.setKind(CompletionItemKind.Property);
		testProvideCamelOptions("<from uri=\"ahc:httpUri?\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 23, completionItem);
	}
    
    @Test
   	void testProvideCamelOptionsWhenAlreadyContainOptions() throws Exception {
    	testProvideCamelOptions("<to uri=\"ahc:httpUri?anOption=aValue&amp;\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 0, 41, getBridgeEndpointExpectedCompletionItem(41,41));
   	}
    
    private void testProvideCamelOptions(String textTotest, int line, int character, CompletionItem completionItemExpected) throws URISyntaxException, InterruptedException, ExecutionException {
    	testProvideCamelOptions(textTotest, line, character, completionItemExpected, ".xml");
    }
       
    private void testProvideCamelOptions(String textTotest, int line, int character, CompletionItem completionItemExpected, String fileType) throws URISyntaxException, InterruptedException, ExecutionException {
    	CamelLanguageServer camelLanguageServer = initializeLanguageServer(textTotest, fileType);
    	
    	CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(line, character));
    	
    	assertThat(completions.get().getLeft()).contains(completionItemExpected);
    }

	private CompletionItem getBridgeEndpointExpectedCompletionItem(int startCharacter, int endCharacter) {
		CompletionItem completionItem = new CompletionItem("bridgeEndpoint");
		completionItem.setInsertText("bridgeEndpoint=false");
    	completionItem.setTextEdit(new TextEdit(new Range(new Position(0, startCharacter), new Position(0, endCharacter)), "bridgeEndpoint=false"));
    	completionItem.setDocumentation("If the option is true, then the Exchange.HTTP_URI header is ignored, and use the endpoint's URI for request. You may also set the throwExceptionOnFailure to be false to let the AhcProducer send all the fault response back.");
    	completionItem.setDetail("boolean");
    	completionItem.setKind(CompletionItemKind.Property);
    	completionItem.setDeprecated(false);
		return completionItem;
	}
    
}
