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
package com.github.cameltooling.lsp.internal.completion.camelapplicationproperties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionItemTag;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.CamelRuntimeProvider;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

/**
 * The configuration properties that Camel JBang and the runtimes add to the ones of Camel Main, loaded from the catalog
 * and from the metadata of the requested runtime version.
 */
class CamelPropertiesRuntimeConfigurationTest extends AbstractCamelLanguageServerTest {

	private static final String FILE_NAME = "application.properties";
	private static final String QUARKUS_VERSION = "3.40.0";
	private static final String SPRING_BOOT_VERSION = "4.23.0";

	private CamelRuntimeProvider runtimeProvider;
	private String runtimeProviderVersion;

	@Test
	void testQuarkusPropertiesCompletedSegmentBySegment() throws Exception {
		useRuntime(CamelRuntimeProvider.QUARKUS, QUARKUS_VERSION);

		List<CompletionItem> firstSegment = getCompletions("quark", 5);
		assertThat(firstSegment).hasSize(1);
		assertThat(firstSegment.get(0).getLabel()).isEqualTo("quarkus");
		assertThat(firstSegment.get(0).getKind()).isEqualTo(CompletionItemKind.Module);
		assertThat(firstSegment.get(0).getDetail()).isEqualTo("Camel Quarkus " + QUARKUS_VERSION);
		assertThat(firstSegment.get(0).getTextEdit().getLeft()).isEqualTo(new TextEdit(new Range(new Position(0, 0), new Position(0, 5)), "quarkus."));

		CompletionItem bootstrap = findCompletion(getCompletions("quarkus.camel.boot", 18), "bootstrap");
		assertThat(bootstrap.getTextEdit().getLeft()).isEqualTo(new TextEdit(new Range(new Position(0, 14), new Position(0, 18)), "bootstrap."));

		CompletionItem enabled = findCompletion(getCompletions("quarkus.camel.bootstrap.", 24), "enabled");
		assertThat(enabled.getKind()).isEqualTo(CompletionItemKind.Property);
		assertThat(enabled.getTextEdit().getLeft()).isEqualTo(new TextEdit(new Range(new Position(0, 24), new Position(0, 24)), "enabled="));
		assertThat(enabled.getDetail()).isEqualTo("Camel Quarkus " + QUARKUS_VERSION);
		assertThat(enabled.getDocumentation().getRight().getValue())
				.contains("When set to true, the `CamelRuntime` will be started automatically.")
				.contains("Type: boolean")
				.contains("Default value: true");
	}

	@Test
	void testGroupCompletedInTheMiddleOfTheKeyKeepsTheNextSegments() throws Exception {
		useRuntime(CamelRuntimeProvider.QUARKUS, QUARKUS_VERSION);

		CompletionItem bootstrap = findCompletion(getCompletions("quarkus.camel.boot.enabled=true", 18), "bootstrap");

		assertThat(bootstrap.getTextEdit().getLeft()).isEqualTo(new TextEdit(new Range(new Position(0, 14), new Position(0, 18)), "bootstrap"));
	}

	@Test
	void testDeprecatedQuarkusProperty() throws Exception {
		useRuntime(CamelRuntimeProvider.QUARKUS, QUARKUS_VERSION);

		CompletionItem routesDiscovery = findCompletion(getCompletions("quarkus.camel.routes-discovery.", 31), "enabled");

		assertThat(routesDiscovery.getTags()).containsExactly(CompletionItemTag.Deprecated);
		assertThat(routesDiscovery.getDocumentation().getRight().getValue()).contains("Deprecated: since 3.26.0");
	}

	@Test
	void testQuarkusEnumValuesCompleted() throws Exception {
		useRuntime(CamelRuntimeProvider.QUARKUS, QUARKUS_VERSION);
		String line = "quarkus.camel.csimple.on-build-time-analysis-failure=";

		List<CompletionItem> completions = getCompletions(line, line.length());

		assertThat(completions).extracting(CompletionItem::getLabel).containsExactly("fail", "ignore", "warn");
	}

	@Test
	void testBooleanValuesCompletedAfterTheSpaces() throws Exception {
		useRuntime(CamelRuntimeProvider.QUARKUS, QUARKUS_VERSION);
		String line = "quarkus.camel.bootstrap.enabled = f";

		List<CompletionItem> completions = getCompletions(line, line.length());

		assertThat(completions).hasSize(1);
		assertThat(completions.get(0).getTextEdit().getLeft()).isEqualTo(new TextEdit(new Range(new Position(0, 34), new Position(0, 35)), "false"));
	}

	@Test
	void testHoverOnQuarkusProperty() throws Exception {
		useRuntime(CamelRuntimeProvider.QUARKUS, QUARKUS_VERSION);

		Hover hover = getHover("quarkus.camel.bootstrap.enabled=false", 10);

		assertThat(hover.getContents().getRight().getValue()).contains("When set to true, the `CamelRuntime` will be started automatically.");
		assertThat(hover.getRange()).isEqualTo(new Range(new Position(0, 0), new Position(0, 31)));
	}

	@Test
	void testSpringBootPropertiesCompletedNextToTheCamelMainOptions() throws Exception {
		useRuntime(CamelRuntimeProvider.SPRINGBOOT, SPRING_BOOT_VERSION);

		List<CompletionItem> completions = getCompletions("camel.main.run", 14);

		assertThat(completions).extracting(CompletionItem::getLabel).contains("run-controller", "name").doesNotContain("main-run-controller");
		CompletionItem runController = findCompletion(completions, "run-controller");
		assertThat(runController.getTextEdit().getLeft()).isEqualTo(new TextEdit(new Range(new Position(0, 11), new Position(0, 14)), "run-controller="));
		assertThat(runController.getDetail()).isEqualTo("Camel Spring Boot " + SPRING_BOOT_VERSION);
	}

	@Test
	void testRuntimePropertiesCompletedFromTheirFirstSegment() throws Exception {
		useRuntime(CamelRuntimeProvider.SPRINGBOOT, SPRING_BOOT_VERSION);

		List<CompletionItem> completions = getCompletions("", 0);

		assertThat(completions).extracting(CompletionItem::getLabel).containsExactly("camel", "management");
	}

	@Test
	void testRuntimeGroupsNextToTheCamelMainGroups() throws Exception {
		useRuntime(CamelRuntimeProvider.SPRINGBOOT, SPRING_BOOT_VERSION);

		List<CompletionItem> completions = getCompletions("camel.", 6);

		assertThat(completions).extracting(CompletionItem::getLabel).contains("jbang", "clustered", "springboot");
		assertThat(completions).filteredOn(item -> "main".equals(item.getLabel())).hasSize(1);
		assertThat(completions).filteredOn(item -> "component".equals(item.getLabel())).hasSize(1);
	}

	@Test
	void testCamelJBangPropertiesCompleted() throws Exception {
		useRuntime(CamelRuntimeProvider.DEFAULT, null);

		List<CompletionItem> groups = getCompletions("camel.", 6);
		CompletionItem jbang = findCompletion(groups, "jbang");
		assertThat(jbang.getDocumentation().getLeft()).isEqualTo("Camel CLI configurations");
		assertThat(jbang.getDetail()).isEqualTo("Camel JBang " + getCatalogVersion());

		CompletionItem runtime = findCompletion(getCompletions("camel.jbang.ru", 14), "runtime");
		assertThat(runtime.getTextEdit().getLeft()).isEqualTo(new TextEdit(new Range(new Position(0, 12), new Position(0, 14)), "runtime="));
		assertThat(runtime.getDocumentation().getRight().getValue()).contains("Possible values: camel-main, spring-boot, quarkus");
	}

	@Test
	void testCamelJBangEnumValuesCompleted() throws Exception {
		useRuntime(CamelRuntimeProvider.DEFAULT, null);
		String line = "camel.jbang.runtime=";

		List<CompletionItem> completions = getCompletions(line, line.length());

		assertThat(completions).extracting(CompletionItem::getLabel).containsExactly("camel-main", "spring-boot", "quarkus");
	}

	@Test
	void testHoverOnCamelJBangProperty() throws Exception {
		useRuntime(CamelRuntimeProvider.DEFAULT, null);

		Hover hover = getHover("camel.jbang.runtime=quarkus", 13);

		assertThat(hover.getContents().getRight().getValue()).contains("Runtime: Camel JBang " + getCatalogVersion());
	}

	@Test
	void testNoRuntimePropertiesWithoutRuntimeVersion() throws Exception {
		useRuntime(CamelRuntimeProvider.QUARKUS, null);

		assertThat(getCompletions("quark", 5)).isEmpty();
	}

	private String getCatalogVersion() throws Exception {
		return camelLanguageServer.getTextDocumentService().getCamelCatalog().get().getCatalogVersion();
	}

	private void useRuntime(CamelRuntimeProvider runtimeProvider, String runtimeProviderVersion) {
		this.runtimeProvider = runtimeProvider;
		this.runtimeProviderVersion = runtimeProviderVersion;
	}

	private List<CompletionItem> getCompletions(String text, int character) throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServer(FILE_NAME, new TextDocumentItem(FILE_NAME, CamelLanguageServer.LANGUAGE_ID, 0, text));
		return getCompletionFor(languageServer, new Position(0, character), FILE_NAME).get().getLeft();
	}

	private Hover getHover(String text, int character) throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServer(FILE_NAME, new TextDocumentItem(FILE_NAME, CamelLanguageServer.LANGUAGE_ID, 0, text));
		return languageServer.getTextDocumentService().hover(new HoverParams(new TextDocumentIdentifier(FILE_NAME), new Position(0, character))).get();
	}

	private CompletionItem findCompletion(List<CompletionItem> completions, String label) {
		return completions.stream().filter(item -> label.equals(item.getLabel())).findFirst()
				.orElseThrow(() -> new AssertionError("No completion " + label));
	}

	@Override
	protected Map<Object, Object> getInitializationOptions() {
		Map<Object, Object> camelSettings = new HashMap<>();
		camelSettings.put(SettingsManager.CATALOG_RUNTIME_PROVIDER, runtimeProvider.name());
		camelSettings.put(SettingsManager.CATALOG_RUNTIME_PROVIDER_VERSION, runtimeProviderVersion);
		Map<Object, Object> initializationOptions = new HashMap<>();
		initializationOptions.put(SettingsManager.TOP_LEVEL_SETTINGS_ID, camelSettings);
		return initializationOptions;
	}
}
