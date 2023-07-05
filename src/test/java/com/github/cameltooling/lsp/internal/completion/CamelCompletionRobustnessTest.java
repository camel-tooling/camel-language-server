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

import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

class CamelCompletionRobustnessTest extends AbstractCamelLanguageServerTest {

	@Test
	void testCompletionOnNotOpenedDocument() throws Exception {
		CamelLanguageServer server = initializeLanguageServer(RouteTextBuilder.createXMLBlueprintRoute(""));
		
		assertThat(getCompletionFor(server, new Position(0, 0), "wrongFileName.txt").get().getLeft()).isEmpty();
	}
	
}
