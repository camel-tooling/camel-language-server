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

import static com.github.cameltooling.lsp.internal.util.RouteTextBuilder.createXMLBlueprintRoute;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelLanguageServerCompletionPositionTest extends AbstractCamelLanguageServerTest {
	
    public static Stream<Arguments> data() {
    	return Stream.of(
    			arguments(createXMLBlueprintRoute(""), 0, 0, -1, -1, false, "Empty URI" ),
    			arguments(createXMLBlueprintRoute(""), 0, 1, -1, -1, false, "Empty URI" ),
    			arguments(createXMLBlueprintRoute(""), 0, 2, -1, -1, false, "Empty URI"),
    			arguments(createXMLBlueprintRoute(""), 0, 3, -1, -1, false, "Empty URI"),
    			arguments(createXMLBlueprintRoute(""), 0, 4, -1, -1, false, "Empty URI"),
    			arguments(createXMLBlueprintRoute(""), 0, 5, -1, -1, false, "Empty URI"),
    			arguments(createXMLBlueprintRoute(""), 0, 6, -1, -1, false, "Empty URI"),
    			arguments(createXMLBlueprintRoute(""), 0, 7, -1, -1, false, "Empty URI"),
    			arguments(createXMLBlueprintRoute(""), 0, 8, -1, -1, false, "Empty URI"),
    			arguments(createXMLBlueprintRoute(""), 0, 9, -1, -1, false, "Empty URI"),
    			arguments(createXMLBlueprintRoute(""), 0, 10, -1, -1, false, "Empty URI" ),
    			arguments(createXMLBlueprintRoute(""), 0, 11, 11, 11, true, "Empty URI" ),
    			arguments(createXMLBlueprintRoute(""), 0, 12, -1, -1, false, "Empty URI" ),
    			arguments(createXMLBlueprintRoute(""), 0, 13, -1, -1, false, "Empty URI" ),
    			arguments(createXMLBlueprintRoute(""), 0, 14, -1, -1, false, "Empty URI" ),
    			arguments(createXMLBlueprintRoute(""), 0, 15, -1, -1, false, "Empty URI" ),

    			arguments(createXMLBlueprintRoute("ahc"), 0, 10, -1, -1, false, "Uri with some value" ),
    			arguments(createXMLBlueprintRoute("ahc"), 0, 11, 11, 14, true, "Uri with some value" ),
    			arguments(createXMLBlueprintRoute("ahc"), 0, 12, 11, 14, true, "Uri with some value" ),
    			arguments(createXMLBlueprintRoute("ahc"), 0, 13, 11, 14, true, "Uri with some value" ),
    			arguments(createXMLBlueprintRoute("ahc"), 0, 14, 11, 14, true, "Uri with some value" ),
    			arguments(createXMLBlueprintRoute("ahc"), 0, 15, -1, -1, false, "Uri with some value" ),

    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 14, 11, 22, true, "Uri with a syntax provided" ),
    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 16, 11, 22, true, "Uri with a syntax provided" ),
    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 17, 11, 22, true, "Uri with a syntax provided" ),
    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 18, 11, 22, true, "Uri with a syntax provided" ),
    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 19, 11, 22, true, "Uri with a syntax provided" ),
    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 20, 11, 22, true, "Uri with a syntax provided" ),
    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 21, 11, 22, true, "Uri with a syntax provided" ),
    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 22, 11, 22, true, "Uri with a syntax provided" ),
    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 23, -1, -1, false, "Uri with a syntax provided" ),
    			arguments(createXMLBlueprintRoute("ahc:httpUri?anOption=aValue"), 0, 24, -1, -1, false, "Uri with a syntax provided" )
    			);
    }
	
    @ParameterizedTest(name="{6} - Position ({1},{2})")
    @MethodSource("data")
	void testProvideCompletionForCamelBlueprintNamespace(
			String textToTest,
			int line,
			int characterCallingCompletion,
			int characterStartCompletion,
			int characterEndCompletion,
			boolean shouldHaveCompletion,
			String testNameQualification) throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textToTest);
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(line, characterCallingCompletion));
		
		if(shouldHaveCompletion) {
			assertThat(completions.get().getLeft()).contains(createExpectedAhcCompletionItem(line, characterStartCompletion, line, characterEndCompletion));
		} else {
			Condition<CompletionItem> ahc = new Condition<>(completionItem -> completionItem.getLabel().contains("ahc"), "Found an ahc component");
			assertThat(completions.get().getLeft().stream()).areNot(ahc);
			assertThat(completions.get().getRight()).isNull();
		}
	}
}
