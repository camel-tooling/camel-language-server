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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.camel.kafkaconnector.catalog.CamelKafkaConnectorCatalog;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelKafkaConnectorTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.RangeChecker;

class CamelKafkaConnectorPropertiesDiagnosticTest extends AbstractDiagnosticTest {

	@Test
	void testInvalidpropertyKey() throws Exception {
		testDiagnostic("ckc-sink-invalid-property", 1);
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		new RangeChecker().check(diagnostic.getRange(), 2, 11, 2, 39);
	}
	
	@Test
	void testInvalidMixOfUrlPropertyAndEndpointProperty() throws Exception {
		testDiagnostic("ckc-sink-invalid-mix-url-and-endpoint", 1);
		Diagnostic diagnostic = lastPublishedDiagnostics.getDiagnostics().get(0);
		new RangeChecker().check(diagnostic.getRange(), 2, 0, 2, 14);
	}
	
	@Test
	void testValidPropertyUrl() throws Exception {
		testDiagnostic("ckc-sink-valid-url", 0);
	}
	
	@Test
	void testValidPropertyKeyDashedCase() throws Exception {
		testDiagnostic("ckc-sink-valid-dashed-case", 0);
	}
	
	@Test
	void testValidPropertyKeyCamelCased() throws Exception {
		testDiagnostic("ckc-sink-valid", 0);
	}
	
	@Test
	void testValidGenericProperty() throws Exception {
		testDiagnostic("ckc-source-valid-with-generic-property", 0);
	}
	
	@Test
	void testInvalidSourcePropertyForSinkConnectorClass() throws Exception {
		testDiagnostic("ckc-sink-invalid-source-property", 1);
	}
	
	@Test
	void testDuplicatedKeyMixingDashedAndCamelCase() throws Exception {
		testDiagnostic("ckc-with-duplicatedKeyMixingDashedAndCamelCase", 2);
	}
	
	private void testDiagnostic(String fileUnderTest, int expectedNumberOfError) throws FileNotFoundException {
		super.testDiagnostic(fileUnderTest, expectedNumberOfError, ".properties");
	}
	
	@Override
	protected CamelLanguageServer initializeLanguageServer(InputStream stream, String extension) {
		CamelLanguageServer languageServer = super.initializeLanguageServer(stream, extension);
		CamelKafkaConnectorCatalog catalog = languageServer.getTextDocumentService().getCamelKafkaConnectorManager().getCatalog();
		catalog.addConnector("connector-source-used-for-test", getContentAsString("/camel-kafka-connector-catalog/connector-source-used-for-test.json"));
		catalog.addConnector("connector-sink-used-for-test", getContentAsString("/camel-kafka-connector-catalog/connector-sink-used-for-test.json"));
		return languageServer;
	}
	
	private String getContentAsString(String pathInBundle) {
		return new BufferedReader(new InputStreamReader(AbstractCamelKafkaConnectorTest.class.getResourceAsStream(pathInBundle), StandardCharsets.UTF_8))
				.lines()
				.map(String::trim)
				.collect(Collectors.joining());
	}
}
