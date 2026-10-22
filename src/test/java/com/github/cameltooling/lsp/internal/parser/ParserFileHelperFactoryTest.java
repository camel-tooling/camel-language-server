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
package com.github.cameltooling.lsp.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ParserFileHelperFactoryTest {

	@ParameterizedTest
	@ValueSource(strings = { "route.camel.yaml", "route.camel.yml", "source.kamelet.yaml", "source.kamelet.yml", "binding.pipe.yaml", "binding.pipe.yml", "binding-pipe.yaml", "binding-pipe.yml" })
	void testCamelYamlDslFileNames(String fileName) {
		TextDocumentItem document = new TextDocumentItem(fileName, "yaml", 1, "spec: {}");
		assertThat(new ParserFileHelperFactory().isCamelYamlDSL(document, fileName)).isTrue();
	}

	@Test
	void testYamlFileWhichIsNotCamel() {
		String fileName = "docker-compose.yaml";
		TextDocumentItem document = new TextDocumentItem(fileName, "yaml", 1, "services: {}");
		assertThat(new ParserFileHelperFactory().isCamelYamlDSL(document, fileName)).isFalse();
	}
}
