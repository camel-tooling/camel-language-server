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

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MultilineCompletionTest extends AbstractCamelLanguageServerTest {


	@Test
	void testMultilineYaml() throws Exception {
		String text = """
apiVersion: camel.apache.org/v1
kind: Integration
metadata:
  name: integration
spec:
  dependencies:
  - mvn:something.something
  flows:
  - route:
      id: timer-amq-log
      from:
        uri: timer:tick
        parameters:
          period: 5000
      steps:
      - to:
          uri: activemq:queue:myQueue
      - to:
          uri: >
            aws2-athena:blabla?label=something
                """;
		CamelLanguageServer languageServer = initializeLanguageServer(text, ".yaml");
		List<CompletionItem> completions =
				getCompletionFor(languageServer, new Position(19, 32))
						.get(1, TimeUnit.SECONDS).getLeft();
		assertThat(completions).isNotEmpty();
	}


}
