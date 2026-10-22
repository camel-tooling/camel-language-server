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
package com.github.cameltooling.lsp.internal.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

class YamlDslDiagnosticTest extends AbstractDiagnosticTest {

	private static final String CAMEL_YAML = ".camel.yaml";

	@Test
	void testValidRoutes() throws Exception {
		testDiagnostic("yaml-dsl-valid", 0, CAMEL_YAML);
	}

	@Test
	void testUnknownOptionInParameters() throws Exception {
		testDiagnostic("yaml-dsl-unknown-option", 1, CAMEL_YAML);
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		checkRange(diagnostic.getRange(), 3, 6, 3, 11);
		assertThat(diagnostic.getSeverity()).isEqualTo(DiagnosticSeverity.Error);
		assertThat(diagnostic.getCode().getLeft()).isEqualTo(DiagnosticService.ERROR_CODE_UNKNOWN_PROPERTIES);
	}

	@Test
	void testInvalidValuesInParameters() throws Exception {
		testDiagnostic("yaml-dsl-invalid-values", 2, CAMEL_YAML);
		List<Diagnostic> diagnostics = lastPublishedDiagnostics.getDiagnostics();
		checkRange(diagnostics.get(0).getRange(), 3, 17, 3, 28);
		assertThat(getDiagnosticMessage(diagnostics.get(0))).isEqualTo("Invalid boolean value: notABoolean");
		checkRange(diagnostics.get(1).getRange(), 8, 19, 8, 26);
		assertThat(diagnostics.get(1).getCode().getLeft()).isEqualTo(DiagnosticService.ERROR_CODE_INVALID_ENUM);
	}

	@Test
	void testUnknownOptionInUri() throws Exception {
		testDiagnostic("yaml-dsl-error-in-uri", 1, CAMEL_YAML);
		checkRange(lastPublishedDiagnostics.getDiagnostics().get(0).getRange(), 3, 21, 3, 27);
	}

	@Test
	void testUnknownComponent() throws Exception {
		testDiagnostic("yaml-dsl-unknown-component", 1, CAMEL_YAML);
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		checkRange(diagnostic.getRange(), 1, 9, 1, 18);
		assertThat(getDiagnosticMessage(diagnostic)).contains("tmer");
	}

	@Test
	void testUnknownStep() throws Exception {
		testDiagnostic("yaml-dsl-unknown-step", 1, CAMEL_YAML);
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		checkRange(diagnostic.getRange(), 3, 8, 3, 14);
		assertThat(diagnostic.getSeverity()).isEqualTo(DiagnosticSeverity.Warning);
	}

	@Test
	void testEndpointsOfSwitch() throws Exception {
		testDiagnostic("yaml-dsl-switch", 2, CAMEL_YAML);
		List<Diagnostic> diagnostics = lastPublishedDiagnostics.getDiagnostics();
		assertThat(diagnostics).extracting(diagnostic -> diagnostic.getRange().getStart().getLine()).containsExactlyInAnyOrder(10, 12);
	}

	@Test
	void testKameletWithPropertyPlaceholders() throws Exception {
		testDiagnostic("yaml-dsl-kamelet", 0, ".kamelet.yaml");
	}

	@Test
	void testPlaceholdersInTheApiOfApiComponents() throws Exception {
		testDiagnostic("yaml-dsl-api-placeholders", 0, CAMEL_YAML);
	}

	@Test
	void testUnknownOptionNextToPlaceholderInTheApiOfApiComponents() throws Exception {
		testDiagnostic("yaml-dsl-api-placeholders-and-errors", 1, CAMEL_YAML);
		checkRange(lastPublishedDiagnostics.getDiagnostics().get(0).getRange(), 8, 14, 8, 22);
	}

	@Test
	void testEndpointOfPipe() throws Exception {
		testDiagnostic("yaml-dsl-pipe", 1, ".pipe.yaml");
		checkRange(lastPublishedDiagnostics.getDiagnostics().get(0).getRange(), 15, 6, 15, 12);
	}

	@Test
	void testYamlWhichIsNotCamel() throws Exception {
		testDiagnostic("yaml-dsl-not-camel", 0, ".yml");
	}

	@Test
	void testInvalidYamlSyntax() throws Exception {
		testDiagnostic("yaml-dsl-invalid-syntax", 0, CAMEL_YAML);
	}
}
