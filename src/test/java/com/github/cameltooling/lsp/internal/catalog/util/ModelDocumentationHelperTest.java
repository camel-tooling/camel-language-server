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
package com.github.cameltooling.lsp.internal.catalog.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.apache.camel.tooling.model.EipModel.EipOptionModel;
import org.eclipse.lsp4j.MarkupKind;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.RuntimeConfigurationProperty;

class ModelDocumentationHelperTest {

	private static final RuntimeConfigurationProperty BOOTSTRAP = new RuntimeConfigurationProperty("Camel Quarkus 3.40.0", "quarkus.camel.bootstrap.enabled",
			"When set to true, the CamelRuntime will be started automatically.", "boolean", "true", Collections.emptyList(), null);

	@Test
	void testMarkdownDetailsAreSeparatedByLineBreaks() {
		String documentation = ModelDocumentationHelper.getConfigurationPropertyDocumentation(BOOTSTRAP, true).getValue();

		assertThat(documentation).isEqualTo("**quarkus.camel.bootstrap.enabled**\n\n"
				+ "When set to true, the CamelRuntime will be started automatically.\n\n"
				+ "**Type:** boolean\\\n"
				+ "**Default value:** true\\\n"
				+ "**Runtime:** Camel Quarkus 3.40.0");
	}

	@Test
	void testPlainTextDetailsAreOnTheirOwnLines() {
		assertThat(ModelDocumentationHelper.getConfigurationPropertyDocumentation(BOOTSTRAP, false))
				.satisfies(documentation -> {
					assertThat(documentation.getKind()).isEqualTo(MarkupKind.PLAINTEXT);
					assertThat(documentation.getValue()).endsWith("Type: boolean\nDefault value: true\nRuntime: Camel Quarkus 3.40.0");
				});
	}

	@Test
	void testMarkdownOptionDocumentationDoesNotEndWithLineBreak() {
		EipOptionModel option = new EipOptionModel();
		option.setDescription("Sets the logging level.");
		option.setJavaType("java.lang.String");
		option.setDefaultValue("INFO");

		assertThat(ModelDocumentationHelper.getOptionDocumentation("loggingLevel", option, true).getValue())
				.endsWith("**Type:** java.lang.String\\\n**Default value:** INFO");
	}
}
