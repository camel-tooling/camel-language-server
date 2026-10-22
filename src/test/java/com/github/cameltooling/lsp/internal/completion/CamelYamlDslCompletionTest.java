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

import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCursorTest;

class CamelYamlDslCompletionTest extends AbstractCursorTest {

	private static final String CAMEL_YAML = ".camel.yaml";

	@Test
	void testStepNames() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - setB§""", CAMEL_YAML))
			.containsExactly("setBody");
	}

	@Test
	void testStepNamesAreOnlyEips() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - §""", CAMEL_YAML))
			.contains("log", "to", "setBody", "choice", "marshal")
			.doesNotContain("simple", "json", "route", "steps");
	}

	@Test
	void testStepNamesInSequenceAtTheIndentationOfItsKey() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				    - lo§""", CAMEL_YAML))
			.contains("log", "loop");
	}

	@Test
	void testEipOptions() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - log:
				          mes§""", CAMEL_YAML))
			.containsExactly("message");
	}

	@Test
	void testOptionsAndExpressionLanguagesOfEipWithExpression() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - setHeader:
				          §""", CAMEL_YAML))
			.contains("name", "expression", "simple", "constant", "jsonpath");
	}

	@Test
	void testExpressionLanguagesInExpression() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - setBody:
				          expression:
				            §""", CAMEL_YAML))
			.contains("simple", "constant", "jsonpath")
			.doesNotContain("id", "expression");
	}

	@Test
	void testDataFormatsOfMarshal() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - marshal:
				          §""", CAMEL_YAML))
			.contains("json", "base64", "id")
			.doesNotContain("dataFormatType");
	}

	@Test
	void testStepsOfWhenClause() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - choice:
				          when:
				            - simple: "${header.foo} == 'bar'"
				              st§""", CAMEL_YAML))
			.containsExactly("steps");
	}

	@Test
	void testKeysOfCaseOfSwitch() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - switch:
				          case:
				            - value: a
				              §""", CAMEL_YAML))
			.contains("uri", "parameters", "value");
	}

	@Test
	void testKeysOfOtherwiseOfSwitchAreNotTheOnesOfChoice() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - switch:
				          otherwise:
				            §""", CAMEL_YAML))
			.containsExactlyInAnyOrder("uri", "parameters");
	}

	@Test
	void testKeysOfRouteAreTheOnesOfTheYamlDsl() throws Exception {
		assertThat(getCompletionLabels("""
				- route:
				    §""", CAMEL_YAML))
			.contains("from", "id")
			.doesNotContain("input", "outputs");
	}

	@Test
	void testEnumValuesOfEipOption() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - log:
				          loggingLevel: §""", CAMEL_YAML))
			.contains("INFO", "DEBUG", "ERROR");
	}

	@Test
	void testBooleanValuesOfEipOption() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - split:
				          parallelProcessing: §""", CAMEL_YAML))
			.containsExactlyInAnyOrder("true", "false");
	}

	@Test
	void testNoCompletionInBlockScalar() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - setBody:
				          constant: |
				            Hello
				            se§""", CAMEL_YAML))
			.isEmpty();
	}

	@Test
	void testEndpointOptionsInUriOfStepShortcut() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - to: "log:out?showA§"
				""", CAMEL_YAML))
			.contains("showAll");
	}

	@Test
	void testNoCompletionInYamlWhichIsNotCamel() throws Exception {
		assertThat(getCompletionLabels("""
				jobs:
				  build:
				    steps:
				      - lo§""", ".yml"))
			.isEmpty();
	}
}
