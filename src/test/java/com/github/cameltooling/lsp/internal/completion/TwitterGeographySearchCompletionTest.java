/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.completion;

import static com.github.cameltooling.lsp.internal.util.RouteTextBuilder.createXMLBlueprintRoute;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class TwitterGeographySearchCompletionTest extends AbstractCamelLanguageServerTest {
	String sep = "&amp;";

	@Test
	void testCompletionHasAllExpectedOptions() throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServer(
				createXMLBlueprintRoute("twitter-search:keywords?"), ".xml");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 35)).get().getLeft();

		assertThat(completions)
				.isNotEmpty().anyMatch(item -> {
					String insertText = item.getInsertText();
					return insertText != null && insertText.contains("latitude=")
							&& insertText.contains(sep + "longitude=")
							&& insertText.contains(sep + "radius=") && insertText.contains(sep + "distanceMetric=");
				});
	}

	@Test
	void testCompletionHasAllExpectedOptionsAfterAnotherOption() throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServer(
				createXMLBlueprintRoute("twitter-search:keywords?accessToken=token" + sep), ".xml");
		List<CompletionItem> completions = getCompletionFor(languageServer, new Position(0, 57)).get().getLeft();

		assertThat(completions)
				.isNotEmpty().anyMatch(item -> {
					String insertText = item.getInsertText();
					return insertText != null && insertText.contains("latitude=")
							&& insertText.contains(sep + "longitude=")
							&& insertText.contains(sep + "radius=") && insertText.contains(sep + "distanceMetric=");
				});
	}
}
