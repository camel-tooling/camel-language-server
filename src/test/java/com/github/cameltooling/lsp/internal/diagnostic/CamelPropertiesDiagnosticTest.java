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

import java.io.FileNotFoundException;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

class CamelPropertiesDiagnosticTest extends AbstractDiagnosticTest {

	@Test
	void testUnknownParameterPropertiesFile() throws Exception {
		testDiagnostic("camel-with-unknownParameter", 1);
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		Range range1 = diagnostic.getRange();
		checkRange(range1, 0, 22, 0, 38);
		assertThat(diagnostic.getMessage()).isEqualTo("Unknown option");
	}
	
	@Test
	void testValidationNoError() throws Exception {
		testDiagnostic("camel-valid", 0);
	}
	
	@Test
	void testValidationSeveralErrors() throws Exception {
		testDiagnostic("camel-with-2-errors", 2);
	}
	
	@Test
	void testInvalidBoolean() throws Exception {
		testDiagnostic("camel-with-boolean-error", 1);
	}
	
	@Test
	void testInvalidInteger() throws Exception {
		testDiagnostic("camel-with-integer-error", 1);
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		assertThat(diagnostic.getRange().getStart().getLine()).isEqualTo(1);
	}
	
	@Test
	void testInvalidEnum() throws Exception {
		testDiagnostic("camel-with-invalid-enum", 1);
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		assertThat(diagnostic.getMessage()).isNotNull();
	}
	
	private void testDiagnostic(String fileUnderTest, int expectedNumberOfError) throws FileNotFoundException {
		super.testDiagnostic(fileUnderTest, expectedNumberOfError, ".properties");
	}
}
