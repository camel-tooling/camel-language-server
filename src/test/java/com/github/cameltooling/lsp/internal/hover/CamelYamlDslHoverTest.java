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
package com.github.cameltooling.lsp.internal.hover;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCursorTest;

class CamelYamlDslHoverTest extends AbstractCursorTest {

	private static final String CAMEL_YAML = ".camel.yaml";
	private static final String TIMER_PERIOD_DOCUMENTATION = "Generate periodic events every period";

	@Test
	void testHoverOnStep() throws Exception {
		assertThat(getHoverContent("""
				- from:
				    uri: timer:tick
				    steps:
				      - setB§ody:
				          constant: Hello
				""", CAMEL_YAML))
			.startsWith("Set Body");
	}

	@Test
	void testHoverOnEipOption() throws Exception {
		assertThat(getHoverContent("""
				- from:
				    uri: timer:tick
				    steps:
				      - log:
				          loggingLev§el: INFO
				""", CAMEL_YAML))
			.startsWith("loggingLevel")
			.contains("logging level");
	}

	@Test
	void testHoverOnExpressionLanguage() throws Exception {
		assertThat(getHoverContent("""
				- from:
				    uri: timer:tick
				    steps:
				      - setBody:
				          expression:
				            sim§ple:
				              expression: Hello
				""", CAMEL_YAML))
			.startsWith("Simple");
	}

	@Test
	void testHoverOnParameter() throws Exception {
		assertThat(getHoverContent("""
				- from:
				    uri: timer:tick
				    parameters:
				      peri§od: 1000
				""", CAMEL_YAML))
			.contains(TIMER_PERIOD_DOCUMENTATION);
	}

	@Test
	void testHoverOnOptionOfUriKeepsEndpointDocumentation() throws Exception {
		assertThat(getHoverContent("""
				- from:
				    uri: "timer:tick?peri§od=1000"
				""", CAMEL_YAML))
			.contains(TIMER_PERIOD_DOCUMENTATION);
	}

	@Test
	void testHoverOnComponentOfStepShortcutKeepsEndpointDocumentation() throws Exception {
		assertThat(getHoverContent("""
				- from:
				    uri: timer:tick
				    steps:
				      - to: "ti§mer:other"
				""", CAMEL_YAML))
			.contains("Generate messages in specified intervals");
	}

	@Test
	void testNoHoverOnValue() throws Exception {
		assertThat(getHoverContent("""
				- from:
				    uri: timer:tick
				    steps:
				      - log:
				          message: Hel§lo
				""", CAMEL_YAML))
			.isNull();
	}

	@Test
	void testNoHoverInYamlWhichIsNotCamel() throws Exception {
		assertThat(getHoverContent("""
				jobs:
				  build:
				    steps:
				      - na§me: build
				""", ".yml"))
			.isNull();
	}
}
