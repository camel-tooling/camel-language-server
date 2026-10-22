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

class CamelYamlParametersCompletionTest extends AbstractCursorTest {

	private static final String CAMEL_YAML = ".camel.yaml";

	@Test
	void testConsumerOptionsForFrom() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: file:inbox
				    parameters:
				      §""", CAMEL_YAML))
			.contains("noop", "delete")
			.doesNotContain("fileExist", "directoryName");
	}

	@Test
	void testProducerOptionsForTo() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - to:
				          uri: file:outbox
				          parameters:
				            §""", CAMEL_YAML))
			.contains("fileExist")
			.doesNotContain("noop", "delete");
	}

	@Test
	void testPathParametersWhenUriIsOnlyTheScheme() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer
				    parameters:
				      §""", CAMEL_YAML))
			.contains("timerName", "period");
	}

	@Test
	void testDefinedParametersAreNotProposedAgain() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    parameters:
				      period: 1000
				      §""", CAMEL_YAML))
			.contains("delay")
			.doesNotContain("period", "timerName");
	}

	@Test
	void testParametersWrittenBeforeTheUri() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - to:
				          parameters:
				            showA§
				          uri: log:out
				""", CAMEL_YAML))
			.containsExactlyInAnyOrder("showAll", "showAllProperties");
	}

	@Test
	void testBooleanValues() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    parameters:
				      fixedRate: §""", CAMEL_YAML))
			.containsExactlyInAnyOrder("true", "false");
	}

	@Test
	void testEnumValues() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - to:
				          uri: log:out
				          parameters:
				            level: §""", CAMEL_YAML))
			.contains("INFO", "DEBUG");
	}

	@Test
	void testKameletPropertiesForKameletUri() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - to:
				          uri: kamelet:log-sink
				          parameters:
				            showH§""", CAMEL_YAML))
			.contains("showHeaders");
	}

	@Test
	void testKameletPropertiesForKameletEip() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - kamelet:
				          name: log-sink
				          parameters:
				            §""", CAMEL_YAML))
			.contains("showHeaders");
	}

	@Test
	void testParametersOfRouteTemplateAreNotEndpointOptions() throws Exception {
		assertThat(getCompletionLabels("""
				- routeTemplate:
				    id: my-template
				    parameters:
				      - name: greeting
				        §""", CAMEL_YAML))
			.containsExactlyInAnyOrder("name", "defaultValue", "description", "required");
	}

	@Test
	void testProducerOptionsForCaseOfSwitch() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - switch:
				          case:
				            - value: a
				              uri: log:out
				              parameters:
				                showA§""", CAMEL_YAML))
			.containsExactlyInAnyOrder("showAll", "showAllProperties");
	}

	@Test
	void testConsumerOptionsForPoll() throws Exception {
		assertThat(getCompletionLabels("""
				- from:
				    uri: timer:tick
				    steps:
				      - poll:
				          uri: file:inbox
				          parameters:
				            §""", CAMEL_YAML))
			.contains("delete", "noop")
			.doesNotContain("fileExist");
	}
}
