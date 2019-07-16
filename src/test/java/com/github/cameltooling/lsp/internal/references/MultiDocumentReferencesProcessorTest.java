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
package com.github.cameltooling.lsp.internal.references;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

public class MultiDocumentReferencesProcessorTest extends AbstractCamelLanguageServerTest {
	
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
	public void testDirectProducerToConsumersReferences() throws Exception {
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
	
	@Test
	public void testDirectConsumerToProducersReferences() throws Exception {
		TextDocumentItem item1 = new TextDocumentItem("uri1.xml", CamelLanguageServer.LANGUAGE_ID, 0, MULTI_DOCUMENT_REFERENCE_DOC2);
		TextDocumentItem item2 = new TextDocumentItem("uri2.xml", CamelLanguageServer.LANGUAGE_ID, 0, MULTI_DOCUMENT_REFERENCE_DOC1);
		Position pos = new Position(3, 34);
		List<? extends Location> results = testRetrieveReferencesFromMultipleOpenedDocuments(".xml", pos, 2, item1, item2);
		
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
				assertThat(line).isIn(5, 9);
				foundInFile2++;
			}
		}
		
		assertThat(foundInFile1).isEqualTo(0);
		assertThat(foundInFile2).isEqualTo(2);
	}
	
	private List<? extends Location> testRetrieveReferencesFromMultipleOpenedDocuments(String suffix, Position posInFirstDoc, int expectedResultCount, TextDocumentItem... documentItems) throws URISyntaxException, InterruptedException, ExecutionException {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(suffix, documentItems);
		CompletableFuture<List<? extends Location>> referencesFuture = getReferencesFor(camelLanguageServer, posInFirstDoc, "uri1.xml");
		List<? extends Location> references = referencesFuture.get();
		assertThat(references).hasSize(expectedResultCount);
		return references;
	}
}
