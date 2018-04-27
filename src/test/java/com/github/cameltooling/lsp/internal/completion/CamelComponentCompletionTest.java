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

@RunWith(Parameterized.class)
public class CamelComponentCompletionTest extends AbstractCamelLanguageServerTest {
	
	@Parameters(name="{4} - Position ({1},{2})")
    public static Collection<Object[]> data() {
    	return Arrays.asList(new Object[][] {
    		
    		// test the component schemes
    		{ "<from uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 		0, 11, "Empty component scheme",			null,	100},
    		{ "<from uri=\"f\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 		0, 12, "URI with component scheme f", 		"f",	1},
    		{ "<from uri=\"fi\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 		0, 13, "URI with component scheme fi",		"fi",	1},
    		{ "<from uri=\"fil\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 14, "URI with component scheme fil", 	"fil",  1},
    		{ "<from uri=\"file\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 15, "URI with component scheme file", 	"file", 1},
    		{ "<from uri=\"file\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 11, "URI with component scheme file", 	null,   100},
    		
    		// test the path params
    		{ "<from uri=\"ahc:\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 		0, 15, "Empty path param", 			"ahc:",     1},
    		{ "<from uri=\"ahc:h\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 		0, 16, "URI with path param h", 	"ahc:h",	1},
    		{ "<from uri=\"ahc:ht\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 		0, 17, "URI with path param ht",	"ahc:ht",	1},
    		{ "<from uri=\"ahc:htt\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 18, "URI with path param htt", 	"ahc:htt",	1},
    		{ "<from uri=\"ahc:http\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 19, "URI with path param http", 	"ahc:http",	1},
    		{ "<from uri=\"ahc:httpUri\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 15, "URI with path param http", 	null,		1},
    		
    		// test the uri options
    		{ "<from uri=\"file:bla?\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 		0, 20, "Empty option",			null,		10},
    		{ "<from uri=\"file:bla?n\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 		0, 21, "URI with option n", 	"n",		1},
    		{ "<from uri=\"file:bla?no\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 22, "URI with option no",	"no",		1},
    		{ "<from uri=\"file:bla?noo\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 23, "URI with option noo", 	"noo",		1},
    		{ "<from uri=\"file:bla?noop\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 24, "URI with option noop", 	"noop",		1},
    		{ "<from uri=\"file:bla?noop\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 20, "URI with option noop", 	null,		10},
    		{ "<from uri=\"file:bla?noop=f\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 26, "URI with option noop", 	"f",	1},
    		{ "<from uri=\"file:bla?noop=t\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 26, "URI with option noop", 	"t",	1},
    		{ "<from uri=\"file:bla?noop=false\" xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 25, "URI with option noop", 	null,	2},
    		
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
    public String filterString;
    @Parameter(5)
    public int expectedMinResultSetSize;
	
	@Test
	public void testProvideCompletionForCamelBlueprintNamespace() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textToTest);
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(line, character));
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items.size()).isGreaterThanOrEqualTo(expectedMinResultSetSize);
		if (filterString != null) {
			for (CompletionItem item : items) {
				assertThat(item.getLabel()).startsWith(filterString);
			}
		} 
		assertThat(completions.get().getRight()).isNull();
	}
}
