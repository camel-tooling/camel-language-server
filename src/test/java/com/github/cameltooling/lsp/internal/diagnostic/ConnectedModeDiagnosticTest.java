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

import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

public class ConnectedModeDiagnosticTest extends AbstractDiagnosticTest {

	@Test
	void testKnativeHintOnXml() throws Exception {
		testDiagnostic("camel-with-knative-endpoint", 1, ".xml");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range, 8, 16, 8, 36);
	}
	
	@Test
	void testKnativeHintOnJava() throws Exception {
		testDiagnostic("camel-with-knative-endpoint", 1, ".java");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range, 12, 14, 12, 34);
	}
	
	@Test
	void testKubernetesHintOnXml() throws Exception {
		testDiagnostic("camel-with-kubernetes-endpoint", 1, ".xml");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range, 8, 16, 8, 60);
	}
	
	@Test
	void testKubernetesHintOnJava() throws Exception {
		testDiagnostic("camel-with-kubernetes-endpoint", 1, ".java");
		Range range = lastPublishedDiagnostics.getDiagnostics().get(0).getRange();
		checkRange(range, 12, 14, 12, 58);
	}
	
}
