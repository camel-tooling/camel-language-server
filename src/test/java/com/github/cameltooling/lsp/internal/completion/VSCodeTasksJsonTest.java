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

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class VSCodeTasksJsonTest extends AbstractCamelLanguageServerTest {

	@Test
	void testCompletionForTrait() throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServerWithFileName("""
			{
				"version": "2.0.0",
				"tasks": [
					{
						"label": "Config for trait properties",
						"type": "camel-k",
						"dev": true,
						"file": "${file}",
						"problemMatcher": [],
						"traits": [""]
					}
				]
			}""", "tasks.json");
		List<CompletionItem> completionItems = getCompletionFor(languageServer, new Position(9, 14), "tasks.json").get().getLeft();
		CompletionItem completionItem = completionItems.stream().filter(ci -> "platform".equals(ci.getLabel())).findAny().get();
		assertThat(completionItem.getDocumentation().getLeft()).isEqualTo("The configuration of Platform trait");
		assertThat(completionItem.getTextEdit().getLeft()).isNotNull();
	}

	@Test
	void testCompletionForTraitProperty() throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServerWithFileName("""
			{
				"version": "2.0.0",
				"tasks": [
					{
						"label": "Config for trait properties",
						"type": "camel-k",
						"dev": true,
						"file": "${file}",
						"problemMatcher": [],
						"traits": ["affinity."]
					}
				]
			}""", "tasks.json");
		List<CompletionItem> completionItems = getCompletionFor(languageServer, new Position(9, 24), "tasks.json").get().getLeft();
		CompletionItem completionItem = completionItems.stream().filter(ci -> "enabled".equals(ci.getLabel())).findAny().get();
		assertThat(completionItem.getDocumentation().getLeft())
				.isEqualTo("Can be used to enable or disable a trait. All traits share this common property.");
		assertThat(completionItem.getTextEdit().getLeft()).isNotNull();
	}

	@Test
	void testNoCompletionOutsideWhenNoTraits() throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServerWithFileName("""
				{
					"version": "2.0.0",
					"tasks": [
						{
							"label": "A label",
							"type": "whatever",
							"dev": true,
							"file": "${file}",
							"problemMatcher": []
						}
					]
				}""", "tasks.json");
		List<CompletionItem> completionItems = getCompletionFor(languageServer, new Position(8, 23), "tasks.json").get()
				.getLeft();
		assertThat(completionItems).isEmpty();
	}

}
