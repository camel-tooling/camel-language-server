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

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class PomCompletionTest extends AbstractCamelLanguageServerTest {

	@Test
	void testQuarkusDebugProfile() throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServerWithFileName("", "pom.xml");
		List<CompletionItem> completionItems = getCompletionFor(languageServer, new Position(0, 0), "pom.xml").get().getLeft();
		assertThat(completionItems).hasSize(1);
		assertThat(completionItems.get(0).getLabel()).isEqualTo("Camel debug profile for Quarkus");
		assertThat(completionItems.get(0).getInsertText()).isNotNull();
	}
	
}
