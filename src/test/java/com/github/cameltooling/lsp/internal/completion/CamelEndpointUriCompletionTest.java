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

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;


class CamelEndpointUriCompletionTest extends AbstractCamelLanguageServerTest {
	
    public static Stream<Arguments> data() {
    	return Stream.of(
    		
    		// test the component schemes - FROM
    		arguments(createXMLBlueprintRoute(""), 		0, 11, "Empty component scheme",			null,	300, ".xml"),
    		arguments(createXMLBlueprintRoute("f"), 	0, 12, "URI with component scheme f", 		"f",	8, ".xml"),
    		arguments(createXMLBlueprintRoute("fi"), 	0, 13, "URI with component scheme fi",		"fi",	1, ".xml"),
    		arguments(createXMLBlueprintRoute("fil"), 	0, 14, "URI with component scheme fil", 	"fil",  1, ".xml"),
    		arguments("<from uri='file' xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 15, "URI with component scheme file using single quote", 	"file", 1, ".xml"),
    		arguments(createXMLBlueprintRoute("file"), 	0, 11, "URI with component scheme file", 	null,   300, ".xml"),
    		arguments(createXMLBlueprintRoute("file"), 	0, 12, "URI with component scheme file", 	"f",    8, ".xml"),
    		arguments("from(\"file\")//camel", 								0, 7, "URI with component scheme file for Java", "f",   8, ".java"),
    		
    		// test the path params - FROM
    		arguments(createXMLBlueprintRoute("ahc"), 			0, 14, "Empty path param without separator",	"ahc",   3, ".xml"),
    		arguments(createXMLBlueprintRoute("ahc:"), 			0, 15, "Empty path param", 			"ahc:",     1, ".xml"),
    		arguments("<from uri='ahc:h' xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 0, 16, "URI with path param h", 	"ahc:h",	1, ".xml"),
    		arguments(createXMLBlueprintRoute("ahc:ht"), 		0, 17, "URI with path param ht",	"ahc:ht",	1, ".xml"),
    		arguments(createXMLBlueprintRoute("ahc:htt"), 		0, 18, "URI with path param htt", 	"ahc:htt",	1, ".xml"),
    		arguments(createXMLBlueprintRoute("ahc:http"), 		0, 19, "URI with path param http", 	"ahc:http",	1, ".xml"),
    		arguments(createXMLBlueprintRoute("ahc:httpUri"), 	0, 15, "URI with path param http", 	null,		1, ".xml"),
    		arguments("from(\"ahc:httpUri\")//camel", 0, 10, "URI with path param http for java", 	null,		1, ".java"),
    		
    		// test the uri options - FROM
    		arguments("from(\"file:bla?\")//camel", 						0, 15, "Empty option for Java",	null,		70, ".java"),
    		arguments(createXMLBlueprintRoute("file:bla?"), 				0, 20, "Empty option",			null,		70, ".xml"),
    		arguments(createXMLBlueprintRoute("file:bla?n"), 				0, 21, "URI with option n", 	"n",		1, ".xml"),
    		arguments(createXMLBlueprintRoute("file:bla?no"), 				0, 22, "URI with option no",	"no",		1, ".xml"),
    		arguments(createXMLBlueprintRoute("file:bla?noo"), 				0, 23, "URI with option noo", 	"noo",		1, ".xml"),
    		arguments("<from uri='file:bla?noop' xmlns=\"http://camel.apache.org/schema/blueprint\"></from>\n", 	0, 24, "URI with option noop", 	"noop",		1, ".xml"),
    		arguments(createXMLBlueprintRoute("file:bla?noop"), 			0, 20, "URI with option noop", 	null,		10, ".xml"),
    		arguments(createXMLBlueprintRoute("file:bla?noop=f"), 			0, 26, "URI with option noop", 	"f",	1, ".xml"),
    		arguments("from(\"file:bla?noop=f\")//camel", 					0, 21, "URI with option noop", 	"f",	1, ".java"),
    		arguments(createXMLBlueprintRoute("file:bla?noop=t"), 			0, 26, "URI with option noop", 	"t",	1, ".xml"),
    		arguments(createXMLBlueprintRoute("file:bla?noop=false"), 		0, 25, "URI with option noop", 	null,	2, ".xml"),
    		arguments(createXMLBlueprintRoute("file:bla?noop=false"),		0, 21, "Param Key Completion",	"n",		1, ".xml"),
    		arguments(createXMLBlueprintRoute("file:bla?noop=false"),		0, 22, "Param Key Completion",	"no",		1, ".xml"),
    		arguments(createXMLBlueprintRoute("file:bla?noop=false&amp;"),	0, 35, "Second option",	null,			70, ".xml"),

    		// test the component schemes - TO
    		arguments("<to uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 		0, 9, "Empty component scheme",			null,	300, ".xml"),
    		arguments("<to uri=\"f\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 		0, 10, "URI with component scheme f", 		"f",	8, ".xml"),
    		arguments("<to uri=\"fi\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 		0, 11, "URI with component scheme fi",		"fi",	1, ".xml"),
    		arguments("<to uri=\"fil\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 	0, 12, "URI with component scheme fil", 	"fil",  1, ".xml"),
    		arguments("<to uri=\"file\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 	0, 13, "URI with component scheme file", 	"file", 1, ".xml"),
    		arguments("<to uri=\"file\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 	0, 9, "URI with component scheme file", 	null,   300, ".xml"),
    		arguments("<to uri=\"file\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 	0, 10, "URI with component scheme file", 	"f",    8, ".xml"),
    		arguments("to(\"file\")//camel", 																0, 5, "URI with component scheme file for Java", "f",   8, ".java"),
    		
    		// test the path params - TO
    		arguments("<to uri=\"ahc:\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 		0, 13, "Empty path param", 			"ahc:",     1, ".xml"),
    		arguments("<to uri=\"ahc:h\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 		0, 14, "URI with path param h", 	"ahc:h",	1, ".xml"),
    		arguments("<to uri='ahc:ht' xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 		0, 15, "URI with path param ht",	"ahc:ht",	1, ".xml"),
    		arguments("<to uri=\"ahc:htt\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 	0, 16, "URI with path param htt", 	"ahc:htt",	1, ".xml"),
    		arguments("<to uri=\"ahc:http\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 	0, 17, "URI with path param http", 	"ahc:http",	1, ".xml"),
    		arguments("<to uri=\"ahc:httpUri\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", 0, 13, "URI with path param http", 	null,		1, ".xml"),
    		arguments("to(\"ahc:httpUri\")//camel", 0, 10, "URI with path param http for java", 	null,		1, ".java"),
    		
    		// test endpoint
    		arguments("<endpoint uri=\"\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 		0, 15, "Empty component scheme",			null,	300, ".xml"),
    		arguments("<endpoint uri=\"f\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 		0, 16, "URI with component scheme f", 		"f",	8, ".xml"),
    		arguments("<endpoint uri=\"fi\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 		0, 17, "URI with component scheme fi",		"fi",	1, ".xml"),
    		arguments("<endpoint uri='fil' xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 18, "URI with component scheme fil", 	"fil",  1, ".xml"),
    		arguments("<endpoint uri=\"file\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 19, "URI with component scheme file", 	"file", 1, ".xml"),
    		arguments("<endpoint uri=\"file\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 15, "URI with component scheme file", 	null,   300, ".xml"),
    		arguments("<endpoint uri=\"file\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 16, "URI with component scheme file", 	"f",    8, ".xml"),
    		// test the endpoint path params
    		arguments("<endpoint uri=\"ahc:\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 		0, 19, "Empty path param", 			"ahc:",     1, ".xml"),
    		arguments("<endpoint uri=\"ahc:h\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 		0, 20, "URI with path param h", 	"ahc:h",	1, ".xml"),
    		arguments("<endpoint uri=\"ahc:ht\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 		0, 21, "URI with path param ht",	"ahc:ht",	1, ".xml"),
    		arguments("<endpoint uri=\"ahc:htt\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 22, "URI with path param htt", 	"ahc:htt",	1, ".xml"),
    		arguments("<endpoint uri='ahc:http' xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 23, "URI with path param http", 	"ahc:http",	1, ".xml"),
    		arguments("<endpoint uri=\"ahc:httpUri\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 0, 19, "URI with path param http", 	null,		1, ".xml"),
    		// endpoint uri options
    		arguments("<endpoint uri=\"file:bla?\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 		0, 24, "Empty option",			null,		70, ".xml"),
    		arguments("<endpoint uri=\"file:bla?n\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 		0, 25, "URI with option n", 	"n",		1, ".xml"),
    		arguments("<endpoint uri=\"file:bla?no\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 26, "URI with option no",	"no",		1, ".xml"),
    		arguments("<endpoint uri='file:bla?noo' xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 27, "URI with option noo", 	"noo",		1, ".xml"),
    		arguments("<endpoint uri=\"file:bla?noop\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 28, "URI with option noop", 	"noop",		1, ".xml"),
    		arguments("<endpoint uri=\"file:bla?noop\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 	0, 24, "URI with option noop", 	null,		10, ".xml"),
    		arguments("<endpoint uri=\"file:bla?noop=f\" xmlns=\"http://camel.apache.org/schema/blueprint\"></endpoint>\n", 0, 30, "URI with option noop", 	"f",	1, ".xml"),
    		
    		//test with prefix
    		arguments("<camel:from uri=\"f\" xmlns:camel=\"http://camel.apache.org/schema/blueprint\"></camel:from>\n", 		0, 18, "URI with component scheme f in file using namespace prefix", 		"f",	8, ".xml")
    	);
    }
	
	@ParameterizedTest(name="{4} - Position ({1},{2}) - {6} - ({0})")
	@MethodSource("data")
	void testProvideCompletionForCamelBlueprintNamespace(String textToTest, int line, int character, String testNameQualification, String filterString, int expectedMinResultSetSize, String extension) throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textToTest, extension);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(line, character));
		List<CompletionItem> items = completions.get().getLeft();
		assertThat(items).hasSizeGreaterThanOrEqualTo(expectedMinResultSetSize);
		if (filterString != null) {
			for (CompletionItem item : items) {
				assertThat(item.getLabel()).startsWith(filterString);
				assertThat(hasTextEdit(item)).isTrue();
			}
		} 
		assertThat(completions.get().getRight()).isNull();
	}
}
