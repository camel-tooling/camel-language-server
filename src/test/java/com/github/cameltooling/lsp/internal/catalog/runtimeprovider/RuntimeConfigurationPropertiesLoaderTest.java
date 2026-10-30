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
package com.github.cameltooling.lsp.internal.catalog.runtimeprovider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.camel.catalog.DefaultCamelCatalog;
import org.junit.jupiter.api.Test;

class RuntimeConfigurationPropertiesLoaderTest {

	private static final String SPRING_BOOT = "Camel Spring Boot 4.23.0";
	private static final String QUARKUS = "Camel Quarkus 3.40.0";

	@Test
	void testSpringBootPropertiesOfCamelMainAreLeftOut() throws Exception {
		List<RuntimeConfigurationProperty> properties = parseSpringBootMetadata();

		assertThat(properties).extracting(RuntimeConfigurationProperty::getName).containsExactly(
				"camel.main.run-controller",
				"camel.main.main-run-controller",
				"camel.ssl.cipher-suites-filter",
				"management.endpoint.camel.access");
	}

	@Test
	void testSpringBootPropertiesAreDocumented() throws Exception {
		Map<String, RuntimeConfigurationProperty> properties = byName(parseSpringBootMetadata());

		RuntimeConfigurationProperty runController = properties.get("camel.main.run-controller");
		assertThat(runController.getRuntime()).isEqualTo(SPRING_BOOT);
		assertThat(runController.getDescription()).isEqualTo("Whether to use the main run controller to ensure the Spring-Boot application keeps running.");
		assertThat(runController.getType()).isEqualTo("java.lang.Boolean");
		assertThat(runController.isBoolean()).isTrue();
		assertThat(runController.getDefaultValue()).isEqualTo("false");
		assertThat(runController.isDeprecated()).isFalse();
		RuntimeConfigurationProperty mainRunController = properties.get("camel.main.main-run-controller");
		assertThat(mainRunController.isDeprecated()).isTrue();
		assertThat(mainRunController.getDeprecationNote()).isEqualTo("since 4.14.0, use camel.main.run-controller instead");
		assertThat(properties.get("management.endpoint.camel.access").getDefaultValue()).isEqualTo("unrestricted");
		assertThat(properties.get("camel.ssl.cipher-suites-filter").getDescription()).isNull();
	}

	@Test
	void testQuarkusPropertiesAreReadFromTheConfigRootsAndTheirSections() throws Exception {
		List<RuntimeConfigurationProperty> properties = parseQuarkusConfigModel();

		assertThat(properties).extracting(RuntimeConfigurationProperty::getName).containsExactly(
				"quarkus.camel.bootstrap.enabled",
				"quarkus.camel.csimple.on-build-time-analysis-failure",
				"quarkus.camel.service.discovery.include-patterns",
				"quarkus.camel.routes-discovery.enabled");
	}

	@Test
	void testQuarkusPropertiesAreDocumented() throws Exception {
		Map<String, RuntimeConfigurationProperty> properties = byName(parseQuarkusConfigModel());

		RuntimeConfigurationProperty bootstrap = properties.get("quarkus.camel.bootstrap.enabled");
		assertThat(bootstrap.getRuntime()).isEqualTo(QUARKUS);
		assertThat(bootstrap.getDescription()).isEqualTo("When set to true, the `CamelRuntime` will be started automatically.");
		assertThat(bootstrap.getType()).isEqualTo("boolean");
		assertThat(bootstrap.isBoolean()).isTrue();
		assertThat(bootstrap.getDefaultValue()).isEqualTo("true");
		assertThat(bootstrap.isDeprecated()).isFalse();
		RuntimeConfigurationProperty csimple = properties.get("quarkus.camel.csimple.on-build-time-analysis-failure");
		assertThat(csimple.getType()).isEqualTo("FailureRemedy");
		assertThat(csimple.getEnumValues()).containsExactly("fail", "ignore", "warn");
		assertThat(properties.get("quarkus.camel.service.discovery.include-patterns").getType()).isEqualTo("list of string");
		RuntimeConfigurationProperty routesDiscovery = properties.get("quarkus.camel.routes-discovery.enabled");
		assertThat(routesDiscovery.isDeprecated()).isTrue();
		assertThat(routesDiscovery.getDeprecationNote()).isEqualTo("since 3.26.0");
		assertThat(routesDiscovery.getDescription()).isNull();
	}

	@Test
	void testQuarkusDescriptionsAreConvertedToMarkdown() throws Exception {
		Map<String, RuntimeConfigurationProperty> properties = byName(parseQuarkusConfigModel());

		assertThat(properties.get("quarkus.camel.service.discovery.include-patterns").getDescription()).isEqualTo(
				"Ant-path style patterns, as described in [the patterns guide](https://example.org/patterns):\n\n"
				+ "```properties\n"
				+ "quarkus.camel.service.discovery.include-patterns = META-INF/services/org/apache/camel/foo/*\n"
				+ "```");
	}

	@Test
	void testAsciidocLinkWithoutTextIsConvertedToTheUrl() {
		assertThat(RuntimeConfigurationPropertiesLoader.toMarkdown("See link:https://example.org[]."))
				.isEqualTo("See https://example.org.");
	}

	@Test
	void testKeysMatchInKebabCaseAndInCamelCase() throws Exception {
		RuntimeConfigurationProperties properties = new RuntimeConfigurationProperties(parseSpringBootMetadata());

		assertThat(properties.find("camel.main.runController")).map(RuntimeConfigurationProperty::getName).contains("camel.main.run-controller");
		assertThat(properties.find("camel.main.run-controller")).isPresent();
		assertThat(properties.find("camel.main.name")).isEmpty();
		assertThat(properties.findStartingWith("camel.main.run")).extracting(RuntimeConfigurationProperty::getName)
				.containsExactly("camel.main.run-controller");
		assertThat(properties.findStartingWith("camel.main.mainRun")).extracting(RuntimeConfigurationProperty::getName)
				.containsExactly("camel.main.main-run-controller");
	}

	@Test
	void testCamelJBangPropertiesAreReadFromTheCatalog() {
		DefaultCamelCatalog catalog = new DefaultCamelCatalog(true);

		RuntimeConfigurationProperties properties = RuntimeConfigurationPropertiesLoader.loadCamelJBang(catalog);

		RuntimeConfigurationProperty runtime = properties.find("camel.jbang.runtime").orElseThrow();
		assertThat(runtime.getRuntime()).isEqualTo("Camel JBang " + catalog.getCatalogVersion());
		assertThat(runtime.getEnumValues()).containsExactly("camel-main", "spring-boot", "quarkus");
		assertThat(properties.find("camel.jbang.health")).get().matches(RuntimeConfigurationProperty::isDeprecated);
		assertThat(properties.getGroupDescription("camel.jbang")).isEqualTo("Camel CLI configurations");
	}

	@Test
	void testPropertiesOfBothKeepTheFirstOnes() throws Exception {
		RuntimeConfigurationProperty jbangRunController = new RuntimeConfigurationProperty("Camel JBang", "camel.main.run-controller", null, null, null, null, null);
		RuntimeConfigurationProperties jbang = new RuntimeConfigurationProperties(List.of(jbangRunController), Map.of("camel.jbang", "Camel CLI configurations"));

		RuntimeConfigurationProperties properties = jbang.with(new RuntimeConfigurationProperties(parseSpringBootMetadata()));

		assertThat(properties.getProperties()).hasSize(4);
		assertThat(properties.find("camel.main.run-controller")).containsSame(jbangRunController);
		assertThat(properties.find("management.endpoint.camel.access")).isPresent();
		assertThat(properties.getGroupDescription("camel.jbang")).isEqualTo("Camel CLI configurations");
	}

	private List<RuntimeConfigurationProperty> parseSpringBootMetadata() throws IOException {
		try (Reader metadata = openTestResource("spring-configuration-metadata.json")) {
			return RuntimeConfigurationPropertiesLoader.parseSpringBootMetadata(SPRING_BOOT, metadata, Arrays.asList("camel.main.name", "camel.main.autoStartup"));
		}
	}

	private List<RuntimeConfigurationProperty> parseQuarkusConfigModel() throws IOException {
		try (Reader model = openTestResource("quarkus-config-model.json"); Reader javadoc = openTestResource("quarkus-config-javadoc.json")) {
			return RuntimeConfigurationPropertiesLoader.parseQuarkusConfigModel(QUARKUS, model, javadoc);
		}
	}

	private Reader openTestResource(String name) {
		return new InputStreamReader(getClass().getResourceAsStream("/runtimeprovider/" + name), StandardCharsets.UTF_8);
	}

	private Map<String, RuntimeConfigurationProperty> byName(List<RuntimeConfigurationProperty> properties) {
		return properties.stream().collect(Collectors.toMap(RuntimeConfigurationProperty::getName, Function.identity()));
	}
}
