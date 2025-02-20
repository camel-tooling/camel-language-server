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
package com.github.cameltooling.lsp.internal.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

class TelemetryTest extends AbstractCamelLanguageServerTest {

	@Test
	void testInitializationWithJvmInfoMetric() throws Exception {
		initializeLanguageServer("");
		assertThat(telemetryEvents).hasSize(1);
		TelemetryEvent telemetryEvent = telemetryEvents.get(0);
		assertThat(telemetryEvent.name).isEqualTo(TelemetryManager.STARTUP_EVENT_NAME);
		Map<String, Object> properties = telemetryEvent.properties;
		assertThat((String)properties.get("jvm.version")).isNotBlank();
	}
	
	@ParameterizedTest(name= "Telemetry sent for language {1}")
	@MethodSource
	void testOpenedDocumentMetric(String content, String extension) throws Exception {
		initializeLanguageServer(content, "."+extension);
		await("Await that telemetry event is sent as it is done async to not slow down the server")
			.untilAsserted(() -> assertThat(telemetryEvents).hasSize(2));
		TelemetryEvent telemetryEvent = telemetryEvents.get(1);
		assertThat(telemetryEvent.name).isEqualTo(TelemetryManager.OPENED_DOCUMENT);
		Map<String, Object> properties = telemetryEvent.properties;
		assertThat((String)properties.get("language")).isEqualTo(extension);
	}
	
	@Test
	void noTelemetrySentForJavaFileWithOnlyCamelTextInIt() throws Exception {
		initializeLanguageServer("camel", ".java");
		assertThat(telemetryEvents).hasSize(1);
	}
	
	private static Stream<Arguments> testOpenedDocumentMetric() {
		InputStream inputStreamOfJavaFile = TelemetryTest.class.getResourceAsStream("/workspace/My3RoutesBuilder.java");
		String javaContent = new BufferedReader(new InputStreamReader(inputStreamOfJavaFile))
				   				.lines()
				   				.collect(Collectors.joining("\n"));
		return Stream.of(
				Arguments.of(RouteTextBuilder.createXMLSpringRoute(""), "xml"),
				Arguments.of(javaContent, "java"),
				Arguments.of("# camel-k:", "yaml")
				);
	}
	
}
