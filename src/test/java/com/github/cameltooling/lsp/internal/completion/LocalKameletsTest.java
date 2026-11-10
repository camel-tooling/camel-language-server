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
package com.github.cameltooling.lsp.internal.completion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.github.cameltooling.lsp.internal.AbstractCursorTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

/**
 * The local Kamelets: the <code>*.kamelet.yaml</code> files of the directories of the <code>local-kamelet-directories</code> setting.
 */
class LocalKameletsTest extends AbstractCursorTest {

	private static final String MY_SINK = """
			apiVersion: camel.apache.org/v1
			kind: Kamelet
			metadata:
			  name: my-sink
			  labels:
			    camel.apache.org/kamelet.type: sink
			spec:
			  definition:
			    title: My Sink
			    description: Send the events to my system
			    required:
			      - endpoint
			    properties:
			      endpoint:
			        title: Endpoint
			        description: The endpoint of my system
			        type: string
			      batchSize:
			        title: Batch size
			        description: The number of events per call
			        type: integer
			        default: 10
			  template:
			    from:
			      uri: kamelet:source
			      steps:
			        - to: log:my-sink
			""";

	private static final String MY_SOURCE = """
			apiVersion: camel.apache.org/v1
			kind: Kamelet
			metadata:
			  name: my-source
			  labels:
			    camel.apache.org/kamelet.type: source
			spec:
			  definition:
			    title: My Source
			    description: Receive the events of my system
			    properties:
			      topic:
			        title: Topic
			        description: The topic to listen to
			        type: string
			  template:
			    from:
			      uri: timer:my-source
			      steps:
			        - to: kamelet:sink
			""";

	private static final String TO_KAMELET_WITH_TEMPLATE_ID = """
			- route:
			    from:
			      uri: timer:tick
			      steps:
			        - to:
			            uri: kamelet
			            parameters:
			              templateId: my-sink
			              §""";

	/** Adds the retries property to {@link #MY_SINK}. */
	private static final String WITH_RETRIES = "      retries:\n        title: Retries\n        type: integer\n      batchSize:";

	@TempDir
	Path workspace;

	private Map<Object, Object> initializationOptions = Collections.emptyMap();

	@Override
	protected Map<Object, Object> getInitializationOptions() {
		return initializationOptions;
	}

	@Test
	void testLocalSinkIsProposedInKameletUriOfTo() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		writeKamelet("kamelets", "my-source", MY_SOURCE);
		setLocalKameletDirectories("./kamelets");

		List<CompletionItem> completions = complete("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: "kamelet:§"
				""");

		assertThat(labels(completions)).contains("my-sink", "aws-kinesis-sink").doesNotContain("my-source");
		assertThat(completions.stream().filter(item -> "my-sink".equals(item.getLabel())).findFirst().get().getDocumentation().getLeft())
			.isEqualTo("Send the events to my system");
	}

	@Test
	void testLocalSourceIsProposedInKameletUriOfFrom() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		writeKamelet("kamelets", "my-source", MY_SOURCE);
		setLocalKameletDirectories("./kamelets");

		assertThat(labels(complete("""
				- route:
				    from:
				      uri: "kamelet:§"
				""")))
			.contains("my-source", "timer-source")
			.doesNotContain("my-sink");
	}

	@Test
	void testPropertiesOfLocalKameletWithTemplateId() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");

		assertThat(labels(complete(TO_KAMELET_WITH_TEMPLATE_ID))).contains("endpoint", "batchSize");
	}

	@Test
	void testPropertiesOfLocalKameletInKameletUri() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");

		assertThat(labels(complete("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: kamelet:my-sink
				            parameters:
				              §""")))
			.contains("endpoint", "batchSize");
	}

	@Test
	void testPropertiesOfLocalKameletInKameletEip() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");

		assertThat(labels(complete("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - kamelet:
				            name: my-sink
				            parameters:
				              §""")))
			.contains("endpoint", "batchSize");
	}

	@Test
	void testPropertiesOfLocalKameletInXmlUri() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");

		assertThat(labels(complete("<to uri=\"kamelet:my-sink?§\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n", "route.xml")))
			.contains("endpoint", "batchSize");
	}

	@Test
	void testHoverOnLocalKamelet() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");

		String text = "<to uri=\"kamelet:my-s§ink\" xmlns=\"http://camel.apache.org/schema/blueprint\"></to>\n";
		CamelLanguageServer languageServer = open("route.xml", text);
		Hover hover = languageServer.getTextDocumentService()
				.hover(new HoverParams(new TextDocumentIdentifier(workspace.resolve("route.xml").toUri().toString()), getCursorPosition(text))).get();

		assertThat(hover.getContents().getLeft().get(0).getLeft()).isEqualTo("Send the events to my system");
	}

	@Test
	void testAbsoluteDirectory() throws Exception {
		Path shared = Files.createDirectories(workspace.resolve("shared-kamelets"));
		Files.writeString(shared.resolve("my-sink.kamelet.yaml"), MY_SINK);
		setLocalKameletDirectories(shared.toString());

		assertThat(labels(complete(TO_KAMELET_WITH_TEMPLATE_ID, "integrations/route.camel.yaml"))).contains("endpoint", "batchSize");
	}

	@Test
	void testRelativeDirectoryIsResolvedAgainstTheDocument() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");

		// integrations/kamelets does not exist
		assertThat(labels(complete(TO_KAMELET_WITH_TEMPLATE_ID, "integrations/route.camel.yaml"))).doesNotContain("endpoint", "batchSize");
	}

	@Test
	void testNoLocalKameletWithoutSetting() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);

		assertThat(labels(complete(TO_KAMELET_WITH_TEMPLATE_ID))).doesNotContain("endpoint", "batchSize");
	}

	@Test
	void testLocalKameletWinsOverTheCatalog() throws Exception {
		writeKamelet("kamelets", "timer-source", MY_SOURCE);
		setLocalKameletDirectories("./kamelets");

		assertThat(labels(complete("""
				- route:
				    from:
				      uri: kamelet:timer-source
				      parameters:
				        §""")))
			.contains("topic")
			.doesNotContain("period", "message");
	}

	@Test
	void testUnsavedContentOfOpenedKameletWins() throws Exception {
		Path kamelet = writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");
		String unsaved = MY_SINK.replace("      batchSize:", WITH_RETRIES);

		CamelLanguageServer languageServer = open("route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID);
		languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
				new TextDocumentItem(kamelet.toUri().toString(), CamelLanguageServer.LANGUAGE_ID, 1, unsaved)));

		assertThat(labels(completeOpened(languageServer, "route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID))).contains("retries", "endpoint");
	}

	@Test
	void testChangedKameletFileIsReadAgain() throws Exception {
		Path kamelet = writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");
		CamelLanguageServer languageServer = open("route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID);
		assertThat(labels(completeOpened(languageServer, "route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID))).doesNotContain("retries");

		Files.writeString(kamelet, MY_SINK.replace("      batchSize:", WITH_RETRIES));
		Files.setLastModifiedTime(kamelet, FileTime.fromMillis(System.currentTimeMillis() + 2000));

		assertThat(labels(completeOpened(languageServer, "route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID))).contains("retries");
	}

	@Test
	void testKameletBeingWrittenIsIgnored() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		writeKamelet("kamelets", "broken", "apiVersion: camel.apache.org/v1\nkind: Kamelet\nspec: [\n");
		writeKamelet("kamelets", "empty", "");
		setLocalKameletDirectories("./kamelets");

		assertThat(labels(complete("""
				- route:
				    from:
				      uri: timer:tick
				      steps:
				        - to:
				            uri: "kamelet:§"
				"""))).contains("my-sink").doesNotContain("broken", "empty");
	}

	@Test
	void testSettingChange() throws Exception {
		writeKamelet("kamelets", "my-sink", MY_SINK);
		CamelLanguageServer languageServer = open("route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID);
		assertThat(labels(completeOpened(languageServer, "route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID))).doesNotContain("endpoint");

		languageServer.getWorkspaceService().didChangeConfiguration(new DidChangeConfigurationParams(
				Map.of("camel", Map.of("local-kamelet-directories", List.of("./kamelets")))));

		assertThat(labels(completeOpened(languageServer, "route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID))).contains("endpoint");
	}

	@Test
	void testKameletCreatedByKaoto() throws Exception {
		// the Kamelet template of Kaoto: spec.template.route, types and dependencies
		writeKamelet("kamelets", "users-source", """
				apiVersion: camel.apache.org/v1
				kind: Kamelet
				metadata:
				  name: users-source
				  annotations:
				    camel.apache.org/kamelet.support.level: "Stable"
				    camel.apache.org/catalog.version: "main-SNAPSHOT"
				    camel.apache.org/kamelet.icon: "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4="
				    camel.apache.org/provider: "Apache Software Foundation"
				    camel.apache.org/kamelet.group: "Users"
				  labels:
				    camel.apache.org/kamelet.type: "source"
				spec:
				  definition:
				    title: users-source
				    description: "Produces periodic events about random users!"
				    type: object
				    properties:
				      period:
				        title: Period
				        description: The time interval between two events
				        type: integer
				        default: 5000
				  types:
				    out:
				      mediaType: application/json
				  dependencies:
				    - "camel:timer"
				    - "camel:http"
				    - "camel:kamelet"
				  template:
				    route:
				      id: route-1922
				      from:
				        id: from-3466
				        uri: timer
				        parameters:
				          period: "{{period}}"
				          timerName: user
				        steps:
				        - to:
				            uri: https
				            parameters:
				              httpUri: random-data-api.com/api/v2/users
				        - to: "kamelet:sink"
				""");
		setLocalKameletDirectories("./kamelets");

		assertThat(labels(complete("""
				- route:
				    from:
				      uri: "kamelet:§"
				"""))).contains("users-source");
		assertThat(labels(complete("""
				- route:
				    from:
				      uri: kamelet
				      parameters:
				        templateId: users-source
				        §"""))).contains("period");
	}

	@Test
	void testDirectoryWithSpace() throws Exception {
		Path kamelets = Files.createDirectories(workspace.resolve("my integrations/kamelets"));
		Files.writeString(kamelets.resolve("my-sink.kamelet.yaml"), MY_SINK);
		setLocalKameletDirectories("./kamelets");

		assertThat(labels(complete(TO_KAMELET_WITH_TEMPLATE_ID, "my integrations/route.camel.yaml"))).contains("endpoint", "batchSize");
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void testUrisOfVSCodeOnWindows() throws Exception {
		Path kamelet = writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");
		// VS Code writes the drive letter in lower case and encodes the colon: file:///c%3A/Users/...
		String routeUri = toVSCodeUri(workspace.resolve("route.camel.yaml"));
		String text = TO_KAMELET_WITH_TEMPLATE_ID.replace(CURSOR, "");
		Files.writeString(workspace.resolve("route.camel.yaml"), text);
		CamelLanguageServer languageServer = initializeLanguageServer(".camel.yaml",
				new TextDocumentItem(routeUri, CamelLanguageServer.LANGUAGE_ID, 0, text));
		languageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(
				new TextDocumentItem(toVSCodeUri(kamelet), CamelLanguageServer.LANGUAGE_ID, 1, MY_SINK.replace("      batchSize:", WITH_RETRIES))));

		assertThat(labels(getCompletionFor(languageServer, getCursorPosition(TO_KAMELET_WITH_TEMPLATE_ID), routeUri).get().getLeft()))
			.contains("endpoint", "retries");
	}

	@Test
	void testDeletedKameletIsForgotten() throws Exception {
		Path kamelet = writeKamelet("kamelets", "my-sink", MY_SINK);
		setLocalKameletDirectories("./kamelets");
		CamelLanguageServer languageServer = open("route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID);
		assertThat(labels(completeOpened(languageServer, "route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID))).contains("endpoint");

		Files.delete(kamelet);

		assertThat(labels(completeOpened(languageServer, "route.camel.yaml", TO_KAMELET_WITH_TEMPLATE_ID))).doesNotContain("endpoint");
	}

	@Test
	void testFirstDirectoryWins() throws Exception {
		writeKamelet("first", "my-sink", MY_SINK);
		writeKamelet("second", "my-sink", MY_SINK.replace("      batchSize:", WITH_RETRIES));
		setLocalKameletDirectories("./first", "./second");

		assertThat(labels(complete(TO_KAMELET_WITH_TEMPLATE_ID))).contains("endpoint").doesNotContain("retries");
	}

	private static String toVSCodeUri(Path path) {
		String uri = path.toUri().toString();
		// file:///C:/... -> file:///c%3A/...
		return "file:///" + Character.toLowerCase(uri.charAt(8)) + "%3A" + uri.substring(10);
	}

	private void setLocalKameletDirectories(String... directories) {
		initializationOptions = Map.of("camel", Map.of("local-kamelet-directories", List.of(directories)));
	}

	private Path writeKamelet(String directory, String name, String content) throws Exception {
		Path kamelets = Files.createDirectories(workspace.resolve(directory));
		return Files.writeString(kamelets.resolve(name + ".kamelet.yaml"), content);
	}

	private List<CompletionItem> complete(String textWithCursor) throws Exception {
		return complete(textWithCursor, "route.camel.yaml");
	}

	private List<CompletionItem> complete(String textWithCursor, String file) throws Exception {
		CamelLanguageServer languageServer = open(file, textWithCursor);
		return completeOpened(languageServer, file, textWithCursor);
	}

	private CamelLanguageServer open(String file, String textWithCursor) throws Exception {
		Path path = workspace.resolve(file);
		Files.createDirectories(path.getParent());
		String text = textWithCursor.replace(CURSOR, "");
		Files.writeString(path, text);
		return initializeLanguageServer(file.substring(file.indexOf('.')),
				new TextDocumentItem(path.toUri().toString(), CamelLanguageServer.LANGUAGE_ID, 0, text));
	}

	private List<CompletionItem> completeOpened(CamelLanguageServer languageServer, String file, String textWithCursor) throws Exception {
		return getCompletionFor(languageServer, getCursorPosition(textWithCursor), workspace.resolve(file).toUri().toString()).get().getLeft();
	}

	private static List<String> labels(List<CompletionItem> completions) {
		return completions.stream().map(CompletionItem::getLabel).collect(Collectors.toList());
	}
}
