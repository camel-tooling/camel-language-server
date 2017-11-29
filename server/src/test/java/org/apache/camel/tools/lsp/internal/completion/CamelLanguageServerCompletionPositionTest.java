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
package org.apache.camel.tools.lsp.internal.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.tools.lsp.internal.AbstractCamelLanguageServerTest;
import org.apache.camel.tools.lsp.internal.CamelLanguageServer;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class CamelLanguageServerCompletionPositionTest extends AbstractCamelLanguageServerTest {
	
	@Parameters(name="{4} - Position ({1},{2})")
    public static Collection<Object[]> data() {
    	return Arrays.asList(new Object[][] {
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 0, false, "Empty URI" },
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 1, false, "Empty URI" },
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 2, false, "Empty URI"},
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 3, false, "Empty URI"},
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 4, false, "Empty URI"},
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 5, false, "Empty URI"},
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 6, false, "Empty URI"},
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 7, false, "Empty URI"},
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 8, false, "Empty URI"},
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 9, false, "Empty URI"},
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 10, false, "Empty URI" },
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 11, true, "Empty URI" },
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 12, false, "Empty URI" },
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 13, false, "Empty URI" },
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 14, false, "Empty URI" },
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 15, false, "Empty URI" },

    		{ "<from uri=\"ahc\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 10, false, "Uri with some value" },
    		{ "<from uri=\"ahc\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 11, true, "Uri with some value" },
    		{ "<from uri=\"ahc\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 12, true, "Uri with some value" },
    		{ "<from uri=\"ahc\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 13, true, "Uri with some value" },
    		{ "<from uri=\"ahc\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 14, true, "Uri with some value" },
    		{ "<from uri=\"ahc\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 15, false, "Uri with some value" },
    		
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 14, true, "Uri with a syntax provided" },
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 16, true, "Uri with a syntax provided" },
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 17, true, "Uri with a syntax provided" },
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 18, true, "Uri with a syntax provided" },
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 19, true, "Uri with a syntax provided" },
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 20, true, "Uri with a syntax provided" },
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 21, true, "Uri with a syntax provided" },
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 22, true, "Uri with a syntax provided" },
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 23, false, "Uri with a syntax provided" },
    		{ "<from uri=\"ahc:httpUri?anOption=aValue\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 24, false, "Uri with a syntax provided" }
    		
    	});
    }
    
    @Parameter
    public String textToTest;
    @Parameter(1)
    public int line;
    @Parameter(2)
    public int character;
    @Parameter(3)
    public boolean shoudlHaveCompletion;
    @Parameter(4)
    public String testNameQualification;
	
	@Test
	public void testProvideCompletionForCamelBlueprintNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textToTest);
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(line, character));
		
		if(shoudlHaveCompletion) {
			assertThat(completions.get().getLeft()).contains(expectedAhcCompletioncompletionItem);
		} else {
			assertThat(completions.get().getLeft()).doesNotContain(expectedAhcCompletioncompletionItem);
			assertThat(completions.get().getRight()).isNull();
		}
	}
}
