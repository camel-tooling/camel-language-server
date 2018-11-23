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
package com.github.cameltooling.lsp.internal.references;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

public class ReferencesProcessorTest extends AbstractCamelLanguageServerTest {

	private static final String SINGLE_REFERENCE = "  <camelContext id=\"myContext\" \r\n" + 
			"    xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <endpoint uri=\"timer:timerName?delay=1000\"></endpoint>\r\n" + 
			"    <route id=\"a route\">\r\n" + 
			"      <from uri='timer:timerName'/>\r\n" + 
			"      <to uri=\"direct:anId\"/>\r\n" + 
			"    </route>\r\n" + 
			"    <route id=\"a second route\">\r\n" + 
			"      <from uri=\"direct:anId\"/>\r\n" + 
			"      <to uri=\"file:directoryName\"/>\r\n" + 
			"    </route>\r\n" + 
			"  </camelContext>";
	
	private static final String SEVERAL_REFERENCES = "  <camelContext id=\"myContext\" \r\n" + 
			"    xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <endpoint uri=\"timer:timerName?delay=1000\"></endpoint>\r\n" + 
			"    <route id=\"a route\">\r\n" + 
			"      <from uri='timer:timerName'/>\r\n" + 
			"      <to uri=\"direct:anId\"/>\r\n" + 
			"    </route>\r\n" + 
			"    <route id=\"a second route\">\r\n" + 
			"      <from uri=\"direct:anId\"/>\r\n" + 
			"      <to uri=\"file:directoryName\"/>\r\n" + 
			"    </route>\r\n" +
			"    <route id=\"a third route\">\r\n" + 
			"      <from uri='timer:timerName'/>\r\n" + 
			"      <to uri=\"direct:anId\"/>\r\n" + 
			"    </route>\r\n" +
			"  </camelContext>";
	
	private static final String SAME_FROM = "  <camelContext id=\"myContext\" \r\n" + 
			"    xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <endpoint uri=\"timer:timerName?delay=1000\"></endpoint>\r\n" + 
			"    <route id=\"a route\">\r\n" + 
			"      <from uri='direct:anId'/>\r\n" + 
			"      <to uri=\"file:directoryName1\"/>\r\n" + 
			"    </route>\r\n" + 
			"    <route id=\"a second route\">\r\n" + 
			"      <from uri=\"direct:anId\"/>\r\n" + 
			"      <to uri=\"file:directoryName2\"/>\r\n" + 
			"    </route>\r\n" +
			"  </camelContext>";
	
	private static final String SAME_TO = "  <camelContext id=\"myContext\" \r\n" + 
			"    xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <endpoint uri=\"timer:timerName?delay=1000\"></endpoint>\r\n" + 
			"    <route id=\"a route\">\r\n" + 
			"      <from uri='file:directoryName1'/>\r\n" + 
			"      <to uri=\"direct:anId\"/>\r\n" + 
			"    </route>\r\n" + 
			"    <route id=\"a second route\">\r\n" + 
			"      <from uri=\"file:directoryName2\"/>\r\n" + 
			"      <to uri=\"direct:anId\"/>\r\n" + 
			"    </route>\r\n" +
			"  </camelContext>";
	
	private static final String DIFFERENTIDS = "  <camelContext id=\"myContext\" \r\n" + 
			"    xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <endpoint uri=\"timer:timerName?delay=1000\"></endpoint>\r\n" + 
			"    <route id=\"a route\">\r\n" + 
			"      <from uri='direct:anId1'/>\r\n" + 
			"      <to uri=\"direct:anId2\"/>\r\n" + 
			"    </route>\r\n" + 
			"    <route id=\"a second route\">\r\n" + 
			"      <from uri=\"direct:anId3\"/>\r\n" + 
			"      <to uri=\"direct:anId4\"/>\r\n" + 
			"    </route>\r\n" +
			"  </camelContext>";

	private static final String SINGLE_REFERENCE_WITH_NAMESPACE_PREFIX = "  <camel:camelContext id=\"myContext\" \r\n" + 
			"    xmlns:camel=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <camel:endpoint uri=\"timer:timerName?delay=1000\"></camel:endpoint>\r\n" + 
			"    <camel:route id=\"a route\">\r\n" + 
			"      <camel:from uri='timer:timerName'/>\r\n" + 
			"      <camel:to uri=\"direct:anId\"/>\r\n" + 
			"    </camel:route>\r\n" + 
			"    <camel:route id=\"a second route\">\r\n" + 
			"      <camel:from uri=\"direct:anId\"/>\r\n" + 
			"      <camel:to uri=\"file:directoryName\"/>\r\n" + 
			"    </camel:route>\r\n" + 
			"  </camel:camelContext>";
	
	private static final String SEVERAL_REFERENCES_WITH_AN_EMPTY_URI ="  <camelContext id=\"myContext\" \r\n" + 
			"    xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
			"    <endpoint uri=\"\"></endpoint>\r\n" + 
			"    <route id=\"a route\">\r\n" + 
			"      <from uri='timer:timerName'/>\r\n" + 
			"      <to uri=\"direct:anId\"/>\r\n" + 
			"    </route>\r\n" + 
			"    <route id=\"a second route\">\r\n" + 
			"      <from uri=\"direct:anId\"/>\r\n" + 
			"      <to uri=\"file:directoryName\"/>\r\n" + 
			"    </route>\r\n" +
			"    <route id=\"a third route\">\r\n" + 
			"      <from uri='timer:timerName'/>\r\n" + 
			"      <to uri=\"direct:anId\"/>\r\n" + 
			"    </route>\r\n" +
			"  </camelContext>";
	
	private static final String MULTI_DOCUMENT_REFERENCE_DOC1 = "<camelContext id=\"cbr-example-context\"\n" + 
			"		xmlns=\"http://camel.apache.org/schema/blueprint\" xmlns:order=\"http://fusesource.com/examples/order/v7\">\n" + 
			"		<route id=\"cbr-route\">\n" + 
			"			<from id=\"from1\" uri=\"file:work/cbr/input\" />\n" + 
			"			<log id=\"log1\" message=\"Receiving order ${file:name}\" />\n" + 
			"			<to id=\"blubber\" uri=\"direct:blubber\" />\n" + 
			"		</route>\n" + 
			"		<route id=\"cbr-route2\">\n" + 
			"			<from id=\"from2\" uri=\"file:work/cbr/input2\" />\n" + 
			"			<to id=\"blubber\" uri=\"direct:blubber\" />\n" + 
			"		</route>\n" +
			"		<route id=\"cbr-route2a\">\n" + 
			"			<from id=\"blubber\" uri=\"direct:blubber\" />\n" + 
			"			<log id=\"log2\" message=\"Receiving order ${file:name}\" />\n" + 
			"			<to id=\"to2\" uri=\"file:work/cbr/output/others1\" />\n" + 
			"		</route>\n" +
			"	</camelContext>";
	
	private static final String MULTI_DOCUMENT_REFERENCE_DOC2 = "<camelContext id=\"cbr-example-context\"\n" + 
			"		xmlns=\"http://camel.apache.org/schema/blueprint\" xmlns:order=\"http://fusesource.com/examples/order/v7\">\n" + 
			"		<route id=\"cbr-route3\">\n" + 
			"			<from id=\"blubber\" uri=\"direct:blubber\" />\n" + 
			"			<log id=\"log2\" message=\"Receiving order ${file:name}\" />\n" + 
			"			<to id=\"to2\" uri=\"file:work/cbr/output/others1\" />\n" + 
			"		</route>\n" + 
			"		<route id=\"cbr-route4\">\n" + 
			"			<from id=\"blubber\" uri=\"direct:blubber\" />\n" + 
			"			<log id=\"log3\" message=\"Receiving order ${file:name}\" />\n" + 
			"			<to id=\"to3\" uri=\"file:work/cbr/output/others1\" />\n" + 
			"		</route>\n" +
			"	</camelContext>";

	@Test
	public void testRetrieveASingleDirectReferenceFor_to() throws Exception {
		Location res = testRetrieveReferences(SINGLE_REFERENCE, 1, new Position(5, 18)).get(0);
		Range range = res.getRange();
		assertThat(range.getStart().getLine()).isEqualTo(8);
		assertThat(range.getEnd().getLine()).isEqualTo(8);
	}
	
	@Test
	public void testRetrieveASingleDirectReferenceFor_to_whenUsingCamelNamespacePrefix() throws Exception {
		Location res = testRetrieveReferences(SINGLE_REFERENCE_WITH_NAMESPACE_PREFIX, 1, new Position(5, 26)).get(0);
		Range range = res.getRange();
		assertThat(range.getStart().getLine()).isEqualTo(8);
		assertThat(range.getEnd().getLine()).isEqualTo(8);
	}
	
	@Test
	public void testRetrieveASingleDirectReferenceFor_from() throws Exception {
		Location res = testRetrieveReferences(SINGLE_REFERENCE, 1, new Position(8, 18)).get(0);
		Range range = res.getRange();
		assertThat(range.getStart().getLine()).isEqualTo(5);
		assertThat(range.getEnd().getLine()).isEqualTo(5);
	}
	
	@Test
	public void testRetrieveSeveralDirectReferenceFor_from() throws Exception {
		testRetrieveReferences(SEVERAL_REFERENCES, 2, new Position(8, 18));
	}
	
	@Test
	public void testRetrieveNoReferenceWithSameFrom() throws Exception {
		testRetrieveReferences(SAME_FROM, 0, new Position(4, 18));
	}
	
	@Test
	public void testRetrieveNoReferenceWithSameTo() throws Exception {
		testRetrieveReferences(SAME_TO, 0, new Position(5, 18));
	}
	
	@Test
	public void testFindReferencesEvenWhenThereisAnEmptyURiSomewhere() throws Exception {
		testRetrieveReferences(SEVERAL_REFERENCES_WITH_AN_EMPTY_URI, 2, new Position(8, 18));
	}
	
	@Test
	public void testRetrieveNoReferenceWithDifferentDirectIds() throws Exception {
		testRetrieveReferences(DIFFERENTIDS, 0, new Position(4, 18));
		testRetrieveReferences(DIFFERENTIDS, 0, new Position(5, 18));
		testRetrieveReferences(DIFFERENTIDS, 0, new Position(8, 18));
		testRetrieveReferences(DIFFERENTIDS, 0, new Position(9, 18));
	}
	
	@Test
	public void testMultiDocumentReferences() throws Exception {
		TextDocumentItem item1 = new TextDocumentItem("uri1.xml", CamelLanguageServer.LANGUAGE_ID, 0, MULTI_DOCUMENT_REFERENCE_DOC1);
		TextDocumentItem item2 = new TextDocumentItem("uri2.xml", CamelLanguageServer.LANGUAGE_ID, 0, MULTI_DOCUMENT_REFERENCE_DOC2);
		Position pos = new Position(5, 25);
		List<? extends Location> results = testRetrieveReferencesFromMultipleOpenedDocuments(".xml", pos, 3, item1, item2);
		
		int foundInFile1 = 0;
		int foundInFile2 = 0;
		
		int lastFoundLine = -1;
		for (Location loc : results) {
			int line = loc.getRange().getStart().getLine();
			if (loc.getUri().equals("uri1.xml")) {
				assertThat(line).isEqualTo(12);
				foundInFile1++;
			} else {
				assertThat(loc.getUri()).isEqualTo("uri2.xml");
				if (lastFoundLine != line) {
					lastFoundLine = line;
				}
				assertThat(line).isIn(3, 8);
				foundInFile2++;
			}
		}
		
		assertThat(foundInFile1).isEqualTo(1);
		assertThat(foundInFile2).isEqualTo(2);
	}
	
	private List<? extends Location> testRetrieveReferencesFromMultipleOpenedDocuments(String suffix, Position posInFirstDoc, int expectedResultCount, TextDocumentItem... documentItems) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(suffix, documentItems);
		CompletableFuture<List<? extends Location>> referencesFuture = getReferencesFor(camelLanguageServer, posInFirstDoc, "uri1.xml");
		List<? extends Location> references = referencesFuture.get();
		assertThat(references).hasSize(expectedResultCount);
		return references;
	}
	
	private List<? extends Location> testRetrieveReferences(String textTotest, int expectedSize, Position position) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(textTotest);
		CompletableFuture<List<? extends Location>> referencesFuture = getReferencesFor(camelLanguageServer, position);
		List<? extends Location> references = referencesFuture.get();
		assertThat(references).hasSize(expectedSize);
		return references;
	}
}
