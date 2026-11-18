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
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCursorTest;

/**
 * The Kamelet names written as YAML values: the templateId of a kamelet URI without path, as Kaoto writes it, and the
 * name of the Kamelet EIP.
 */
class CamelYamlKameletNameTest extends AbstractCursorTest {

	private static final String CAMEL_YAML = ".camel.yaml";

	@Test
	void testSinkAndActionKameletsForTemplateIdInTo() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet
				            parameters:
				              templateId: §""", CAMEL_YAML))
			.contains("log-sink", "delay-action")
			.doesNotContain("timer-source");
	}

	@Test
	void testSourceKameletsForTemplateIdInFrom() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    from:
				      uri: kamelet
				      parameters:
				        templateId: §""", CAMEL_YAML))
			.contains("timer-source")
			.doesNotContain("log-sink", "delay-action");
	}

	@Test
	void testTemplateIdWrittenBeforeTheUri() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            parameters:
				              templateId: §
				            uri: "kamelet"
				""", CAMEL_YAML))
			.contains("log-sink", "delay-action");
	}

	@Test
	void testKebabCaseTemplateId() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet
				            parameters:
				              template-id: §""", CAMEL_YAML))
			.contains("log-sink");
	}

	@Test
	void testCompletionReplacesTheTypedName() throws Exception {
		List<CompletionItem> completions = getCompletions("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet
				            parameters:
				              templateId: dela§y-act # a comment""", CAMEL_YAML);

		CompletionItem delayAction = findItem(completions, "delay-action");
		assertThat(delayAction.getTextEdit().getLeft())
			.isEqualTo(new TextEdit(new Range(new Position(7, 26), new Position(7, 35)), "delay-action"));
		assertThat(delayAction.getDocumentation().getLeft()).isEqualTo("Delay the processing using a specific amount of time");
	}

	@Test
	void testCompletionInsideQuotes() throws Exception {
		List<CompletionItem> completions = getCompletions("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet
				            parameters:
				              templateId: "dela§"
				""", CAMEL_YAML);

		assertThat(findItem(completions, "delay-action").getTextEdit().getLeft())
			.isEqualTo(new TextEdit(new Range(new Position(7, 27), new Position(7, 31)), "delay-action"));
	}

	@Test
	void testCompletionRightAfterTheSeparatorAddsTheSpace() throws Exception {
		List<CompletionItem> completions = getCompletions("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet
				            parameters:
				              templateId:§""", CAMEL_YAML);

		assertThat(findItem(completions, "log-sink").getTextEdit().getLeft())
			.isEqualTo(new TextEdit(new Range(new Position(7, 25), new Position(7, 25)), " log-sink"));
	}

	@Test
	void testSinkAndActionKameletsForNameOfKameletEip() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - kamelet:
				            name: §""", CAMEL_YAML))
			.contains("log-sink", "delay-action")
			.doesNotContain("timer-source");
	}

	@Test
	void testNoKameletForTemplateIdOfAnotherComponent() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: log
				            parameters:
				              templateId: §""", CAMEL_YAML))
			.doesNotContain("log-sink", "delay-action");
	}

	@Test
	void testNoKameletForTemplateIdOfKameletUriWithPath() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet:log-sink
				            parameters:
				              templateId: §""", CAMEL_YAML))
			.doesNotContain("log-sink", "delay-action");
	}

	@Test
	void testNoKameletForNameOfAnotherStep() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - setHeader:
				            name: §""", CAMEL_YAML))
			.doesNotContain("log-sink", "delay-action");
	}

	@Test
	void testKeysAreStillProposedInParametersOfKameletUri() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet
				            parameters:
				              templateId: delay-action
				              §""", CAMEL_YAML))
			.contains("milliseconds")
			.doesNotContain("log-sink", "delay-action");
	}

	@Test
	void testHoverOnTemplateId() throws Exception {
		assertThat(getHoverContent("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet
				            parameters:
				              templateId: delay-ac§tion
				""", CAMEL_YAML))
			.isEqualTo("Delay the processing using a specific amount of time");
	}

	@Test
	void testHoverOnNameOfKameletEip() throws Exception {
		assertThat(getHoverContent("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - kamelet:
				            name: "log-s§ink"
				""", CAMEL_YAML))
			.isEqualTo("A sink that logs all data that it receives, useful for debugging purposes.");
	}

	@Test
	void testHoverOnUnknownKamelet() throws Exception {
		assertThat(getHoverContent("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet
				            parameters:
				              templateId: unkn§own
				""", CAMEL_YAML))
			.isNull();
	}

	@Test
	void testHoverOnTemplateIdKeyStillDocumentsTheOption() throws Exception {
		assertThat(getHoverContent("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet
				            parameters:
				              templat§eId: delay-action
				""", CAMEL_YAML))
			.contains("The Route Template ID");
	}

	private static CompletionItem findItem(List<CompletionItem> completions, String label) {
		return completions.stream().filter(item -> label.equals(item.getLabel())).findFirst().orElseThrow();
	}
}
