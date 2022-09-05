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

import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class KameletCompletionTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testKameletTemplateIdCompletionForSource() throws Exception {
		CamelLanguageServer languageServer = initLanguageServer(createXMLBlueprintRoute("kamelet:"));
		
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 19)).get().getLeft();
		
		CompletionItem completionItem = completions.stream().filter(completion -> completion.getLabel().equals("aws-ddb-streams-source")).findAny().get();
		System.out.println("$$"+completionItem.getDocumentation().getLeft()+"$$");
		
		assertThat(completions)
			.hasSizeGreaterThan(10)
			.contains(createAwsddbSourceCompletionItem())
			.doesNotContain(createAwsKinesisSinkCompletionItem());
	}
	
	@Test
	void testKameletTemplateIdCompletionForSink() throws Exception {
		CamelLanguageServer languageServer = initLanguageServer("<to   uri=\"kamelet:\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n");
		
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 19)).get().getLeft();
		
		assertThat(completions)
			.hasSizeGreaterThan(10)
			.contains(createAwsKinesisSinkCompletionItem())
			.doesNotContain(createAwsddbSourceCompletionItem());
	}
	
	@Test
	void testKameletPropertyCompletionForSource() throws Exception {
		CamelLanguageServer languageServer = initLanguageServer(createXMLBlueprintRoute("kamelet:aws-ddb-streams-source?"));
		
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 42)).get().getLeft();
		
		assertThat(completions)
			.hasSizeGreaterThan(5)
			.contains(createTablePropertyCompletionItem())
			.contains(createPropertyCompletionItemWithDefaultValue());
	}
	
	@Test
	void testKameletPropertyCompletionWithTypeForSource() throws Exception {
		CamelLanguageServer languageServer = initLanguageServer(createXMLBlueprintRoute("kamelet:aws-ddb-streams-source?secretKey"));
		
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 49)).get().getLeft();
		
		assertThat(completions)
			.hasSize(1)
			.contains(createSecretKeyPropertyCompletionItem());
	}

	private CompletionItem createSecretKeyPropertyCompletionItem() {
		CompletionItem completionItem = new CompletionItem("secretKey");
		completionItem.setInsertText("secretKey=");
		completionItem.setDetail("string");
		completionItem.setTextEdit(
				Either.forLeft(
						new TextEdit(
								new Range(new Position(0, 42), new Position(0, 51)),
								"secretKey=")));
		completionItem.setDocumentation("The secret key obtained from AWS.");
		return completionItem;
	}
	
	private CompletionItem createTablePropertyCompletionItem() {
		CompletionItem completionItem = new CompletionItem("table");
		completionItem.setInsertText("table=");
		completionItem.setDetail("string");
		completionItem.setTextEdit(
				Either.forLeft(
						new TextEdit(
								new Range(new Position(0, 42), new Position(0, 42)),
								"table=")));
		completionItem.setDocumentation("The name of the DynamoDB table.");
		return completionItem;
	}
	
	private CompletionItem createPropertyCompletionItemWithDefaultValue() {
		CompletionItem completionItem = new CompletionItem("streamIteratorType");
		completionItem.setInsertText("streamIteratorType=FROM_LATEST");
		completionItem.setDetail("string");
		completionItem.setTextEdit(
				Either.forLeft(
						new TextEdit(
								new Range(new Position(0, 42), new Position(0, 42)),
								"streamIteratorType=FROM_LATEST")));
		completionItem.setDocumentation("Defines where in the DynamoDB stream to start getting records. There are two enums and the value can be one of FROM_LATEST and FROM_START. Note that using FROM_START can cause a significant delay before the stream has caught up to real-time.");
		return completionItem;
	}

	private CompletionItem createAwsddbSourceCompletionItem() {
		CompletionItem completionItem = new CompletionItem("aws-ddb-streams-source");
		completionItem.setTextEdit(
				Either.forLeft(
						new TextEdit(
								new Range(new Position(0, 19), new Position(0, 19)),
								"aws-ddb-streams-source")));
		completionItem.setDocumentation("Receive events from Amazon DynamoDB Streams.\n"
				+ "\n"
				+ "The basic authentication method for the AWS DynamoDB Streams service is to specify an access key and a secret key. These parameters are optional because the Kamelet provides a default credentials provider.\n"
				+ "\n"
				+ "If you use the default credentials provider, the DynamoDB Streams client loads the credentials through this provider and doesn't use the basic authentication method.");
		return completionItem;
	}
	
	private CompletionItem createAwsKinesisSinkCompletionItem() {
		CompletionItem completionItem = new CompletionItem("aws-kinesis-sink");
		completionItem.setTextEdit(
				Either.forLeft(
						new TextEdit(
								new Range(new Position(0, 19), new Position(0, 19)),
								"aws-kinesis-sink")));
		completionItem.setDocumentation("Send data to AWS Kinesis.\n"
				+ "\n"
				+ "The basic authentication method for the Kinesis service is to specify an access key and a secret key. These parameters are optional because the Kamelet provides a default credentials provider.\n"
				+ "\n"
				+ "If you use the default credentials provider, the Kinesis client loads the credentials through this provider and doesn't use the basic authentication method.\n"
				+ "\n"
				+ "In the header, you can optionally set the `file` / `ce-partition` property to set the Kinesis partition key.\n"
				+ "\n"
				+ "If you do not set the property in the header, the Kamelet uses the exchange ID for the partition key.\n"
				+ "\n"
				+ "You can also set the `sequence-number` / `ce-sequencenumber` property in the header to specify the Sequence number.");
		return completionItem;
	}
		
	private CamelLanguageServer initLanguageServer(String text) throws URISyntaxException, InterruptedException, ExecutionException {
		return initializeLanguageServer(text, ".xml");
	}
	
}
