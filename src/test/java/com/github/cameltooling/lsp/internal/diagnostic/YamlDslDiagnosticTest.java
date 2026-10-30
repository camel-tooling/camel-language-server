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
import org.eclipse.lsp4j.DiagnosticTag;
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

	@Test
	void testValidStructure() throws Exception {
		testDiagnostic("yaml-dsl-structure-valid", 0, CAMEL_YAML);
	}

	@Test
	void testUnknownAndMissingOptionsOfEip() throws Exception {
		testDiagnostic("yaml-dsl-structure-unknown-option", 2, CAMEL_YAML);
		List<Diagnostic> diagnostics = lastPublishedDiagnostics.getDiagnostics();
		assertThat(getDiagnosticMessage(diagnostics.get(0))).isEqualTo("Unknown option 'mesage' for 'log'");
		assertThat(diagnostics.get(0).getSeverity()).isEqualTo(DiagnosticSeverity.Warning);
		checkRange(diagnostics.get(0).getRange(), 4, 10, 4, 16);
		assertThat(getDiagnosticMessage(diagnostics.get(1))).isEqualTo("Missing required option 'message' for 'log'");
		checkRange(diagnostics.get(1).getRange(), 3, 8, 3, 11);
	}

	@Test
	void testInvalidEnumValueOfEip() throws Exception {
		testDiagnostic("yaml-dsl-structure-invalid-enum", 1, CAMEL_YAML);
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		assertThat(getDiagnosticMessage(diagnostic)).startsWith("Invalid value 'VERBOSE' for 'loggingLevel'");
		checkRange(diagnostic.getRange(), 5, 24, 5, 31);
	}

	@Test
	void testMissingExpression() throws Exception {
		testDiagnostic("yaml-dsl-structure-missing-expression", 2, CAMEL_YAML);
		List<Diagnostic> diagnostics = lastPublishedDiagnostics.getDiagnostics();
		assertThat(getDiagnosticMessage(diagnostics.get(0))).isEqualTo("Missing required option 'expression' for 'setBody'");
		checkRange(diagnostics.get(0).getRange(), 3, 8, 3, 15);
		// a misspelled language is reported instead of the missing expression
		assertThat(getDiagnosticMessage(diagnostics.get(1))).isEqualTo("Unknown option 'simpel' for 'setHeader'");
		checkRange(diagnostics.get(1).getRange(), 7, 10, 7, 16);
	}

	@Test
	void testMisplacedElements() throws Exception {
		testDiagnostic("yaml-dsl-structure-misplaced", 3, CAMEL_YAML);
		List<Diagnostic> diagnostics = lastPublishedDiagnostics.getDiagnostics();
		assertThat(getDiagnosticMessage(diagnostics.get(0))).isEqualTo("Unknown Camel YAML DSL element: 'steps'");
		checkRange(diagnostics.get(0).getRange(), 2, 2, 2, 7);
		assertThat(getDiagnosticMessage(diagnostics.get(1))).isEqualTo("Unknown option 'stps' for 'otherwise'");
		checkRange(diagnostics.get(1).getRange(), 14, 14, 14, 18);
		// unlike the options, the steps cannot be written in kebab case
		assertThat(getDiagnosticMessage(diagnostics.get(2))).isEqualTo("Unknown Camel EIP or step: 'set-body'");
		checkRange(diagnostics.get(2).getRange(), 16, 10, 16, 18);
	}

	@Test
	void testRouteConfigurationAndDeprecatedLanguage() throws Exception {
		testDiagnostic("yaml-dsl-structure-route-configuration", 2, CAMEL_YAML);
		List<Diagnostic> diagnostics = lastPublishedDiagnostics.getDiagnostics();
		assertThat(getDiagnosticMessage(diagnostics.get(0))).isEqualTo("Unknown option 'onExceptionn' for 'onException'");
		checkRange(diagnostics.get(0).getRange(), 10, 8, 10, 20);
		Diagnostic deprecated = diagnostics.get(1);
		assertThat(getDiagnosticMessage(deprecated)).isEqualTo("'ognl' is deprecated");
		assertThat(deprecated.getSeverity()).isEqualTo(DiagnosticSeverity.Hint);
		assertThat(deprecated.getTags()).containsExactly(DiagnosticTag.Deprecated);
		checkRange(deprecated.getRange(), 17, 10, 17, 14);
	}

	@Test
	void testStructureOfKameletTemplate() throws Exception {
		testDiagnostic("yaml-dsl-structure-kamelet", 1, ".kamelet.yaml");
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		assertThat(getDiagnosticMessage(diagnostic)).isEqualTo("Unknown Camel EIP or step: 'lgo'");
		checkRange(diagnostic.getRange(), 11, 10, 11, 13);
	}

	@Test
	void testRemovedElementsWithBundledCatalog() throws Exception {
		testDiagnostic("yaml-dsl-structure-older-version", 5, CAMEL_YAML);
		assertThat(lastPublishedDiagnostics.getDiagnostics()).extracting(this::getDiagnosticMessage).containsExactly(
				"Unknown option 'pattern' for 'wireTap'",
				"Unknown Camel EIP or step: 'serviceCall'",
				"Unknown option 'csimple' for 'setBody'",
				"Unknown Camel EIP or step: 'setBdy'",
				"Unknown option 'toType' for 'transform'");
	}
}
