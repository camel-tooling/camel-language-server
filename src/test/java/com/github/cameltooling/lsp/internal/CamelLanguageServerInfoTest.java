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
package com.github.cameltooling.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.ServerInfo;
import org.junit.jupiter.api.Test;

class CamelLanguageServerInfoTest extends AbstractCamelLanguageServerTest {

	@Test
	void testServerInfoInInitializeResult() throws Exception {
		CamelLanguageServer languageServer = new CamelLanguageServer();
		languageServer.connect(new DummyLanguageClient());
		InitializeParams params = new InitializeParams();
		params.setInitializationOptions(Collections.emptyMap());

		ServerInfo serverInfo = languageServer.initialize(params).get().getServerInfo();

		assertThat(serverInfo.getName()).isEqualTo("Apache Camel Language Server");
		assertThat(serverInfo.getVersion()).isNotEmpty();
	}
}
