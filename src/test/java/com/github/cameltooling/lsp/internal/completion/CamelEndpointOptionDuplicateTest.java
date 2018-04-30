/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

/**
 * @author lheinema
 */
@RunWith(Parameterized.class)
public class CamelEndpointOptionDuplicateTest extends AbstractCamelLanguageServerTest {
	
	@Parameters(name="{4} - Position ({1},{2})")
    public static Collection<Object[]> data() {
    	return Arrays.asList(new Object[][] {
    		
    		// test for duplicate filtering of uri params
    		{ "<from uri=\"file:bla?noop=false&amp;\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 						0, 35, "URI with duplicate option noop", 					"noop" },
    		{ "<from uri=\"file:bla?noop=false&amp;recursive=true&amp;\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 54, "URI with duplicate option recursive", 				"recursive" },
    		{ "<from uri=\"file:bla?noop=false&amp;recursive=true&amp;\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 54, "URI with duplicate option noop after recursive", 	"noop" },
    		
    	});
    }
    
    @Parameter
    public String textToTest;
    @Parameter(1)
    public int line;
    @Parameter(2)
    public int character;
    @Parameter(3)
    public String testNameQualification;
    @Parameter(4)
    public String excludedString;
	
	@Test
	public void testProvideCompletionForCamelBlueprintNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textToTest);
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(line, character));
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items.size()).isGreaterThan(0);
		if (excludedString != null) {
			for (CompletionItem item : items) {
				assertThat(item.getLabel()).doesNotStartWith(excludedString);
			}
		} 
		assertThat(completions.get().getRight()).isNull();
	}
}
