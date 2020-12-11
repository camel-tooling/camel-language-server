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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class CamelKafkaConnectorOfficialExamplesDiagnosticTest extends AbstractDiagnosticTest {

	@TempDir
	static Path folderWithExamples;
	
	private static String CAMEL_KAFKA_CONNECTOR_VERSION = System.getProperty("camel.kafka.connector.version");

	private static Git mainGitrepo;
	private static Git specificExampleGitrepo;

	private static File mainRepoDirectory;
	private static File specificExampleRepoDirectory;
	
	@BeforeAll
	public static void beforeAll() throws InvalidRemoteException, TransportException, GitAPIException {
		assertThat(CAMEL_KAFKA_CONNECTOR_VERSION)
			.as("The Camel Kafka Connector version needs to be set as JVM property to play this test. It is done automatically when calling from Maven.")
			.isNotNull();
		mainRepoDirectory = new File(folderWithExamples.toFile(), "camel-kafka-connector");
		mainGitrepo = Git.cloneRepository()
			.setURI("https://github.com/apache/camel-kafka-connector")
			.setDirectory(mainRepoDirectory)
			.setBranch("refs/tags/camel-kafka-connector-"+ CAMEL_KAFKA_CONNECTOR_VERSION)
			.call();
		
		specificExampleRepoDirectory = new File(folderWithExamples.toFile(), "camel-kafka-connector-examples");
		specificExampleGitrepo = Git.cloneRepository()
				.setURI("https://github.com/apache/camel-kafka-connector-examples")
				.setDirectory(specificExampleRepoDirectory)
				.call();
	}
	
	@AfterAll
	public static void afterAll() {
		if(mainGitrepo != null) {
			mainGitrepo.close();
		}
		if(specificExampleGitrepo != null) {
			specificExampleGitrepo.close();
		}
	}
	
	@ParameterizedTest
	@MethodSource
	void testMainGitRepoExamples(File fileExample) throws Exception {
		testDiagnostic(fileExample, 0, ".properties");
	}
	
	@ParameterizedTest
	@MethodSource
	void testSpecificGitRepoExamples(File fileExample) throws Exception {
		testDiagnostic(fileExample, 0, ".properties");
	}
	
	@ValueSource
	static Stream<File> testMainGitRepoExamples() {
		File[] exampleFiles = new File(mainRepoDirectory, "examples").listFiles();
		assertThat(exampleFiles).as("Allows to detect if examples has moved for instance.").hasSizeGreaterThan(14);
		return Stream.of(exampleFiles)
				// filtering example due to https://github.com/apache/camel-kafka-connector/issues/767
				.filter(file -> !"CamelAmqpSourceConnector.properties".equals(file.getName()));
	}
	
	@ValueSource
	static Stream<File> testSpecificGitRepoExamples() throws IOException {
		return Files.walk(specificExampleRepoDirectory.toPath())
				.map(Path::toFile)
				.filter(file -> file.getName().endsWith(".properties"))
				// filtering example due to https://issues.apache.org/jira/browse/CAMEL-15938
				.filter(file -> !file.getAbsolutePath().contains("aws2-iam"));
	}
	
}
