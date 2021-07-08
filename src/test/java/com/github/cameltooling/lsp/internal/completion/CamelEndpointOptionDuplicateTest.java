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
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

/**
 * @author lheinema
 */
class CamelEndpointOptionDuplicateTest extends AbstractCamelLanguageServerTest {
	
    public static Stream<Arguments> data() {
    	return Stream.of(
    		// test for duplicate filtering of uri params
    		arguments(RouteTextBuilder.createXMLBlueprintRoute("file:bla?noop=false&amp;"), 					0, 35, "URI with duplicate option noop", 					"noop"),
    		arguments(RouteTextBuilder.createXMLBlueprintRoute("file:bla?noop=false&amp;recursive=true&amp;"), 	0, 54, "URI with duplicate option recursive", 				"recursive"),
    		arguments(RouteTextBuilder.createXMLBlueprintRoute("file:bla?noop=false&amp;recursive=true&amp;"), 	0, 54, "URI with duplicate option noop after recursive", 	"noop")
    	);
    }
    	
	@ParameterizedTest(name="{4} - Position ({1},{2})")
	@MethodSource("data")
	void testProvideCompletionForCamelBlueprintNamespace(String textToTest, int line, int character, String testNameQualification, String excludedString) throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textToTest);
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(line, character));
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items.size()).isPositive();
		if (excludedString != null) {
			for (CompletionItem item : items) {
				assertThat(item.getLabel()).doesNotStartWith(excludedString);
			}
		} 
		assertThat(completions.get().getRight()).isNull();
	}
}
