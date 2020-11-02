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

import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.Test;

class CamelKModelineDiagnosticTest extends AbstractDiagnosticTest {

	@Test
	void testDuplicatedTraitsInModeline() throws Exception {
		testDuplicatedTraitsInModeline("camelk-with-duplicated-traits-in-modeline", 0);
	}
	
	@Test
	void testDuplicatedTraitsInModelineWrittenOnSecondLIne() throws Exception {
		testDuplicatedTraitsInModeline("camelk-with-duplicated-traits-in-modeline-written-secondline", 1);
	}

	private void testDuplicatedTraitsInModeline(String fileUnderTest, int lineNumber) throws FileNotFoundException {
		testDiagnostic(fileUnderTest, 2, ".java");
		List<Diagnostic> diagnostics = lastPublishedDiagnostics.getDiagnostics();
		diagnostics.sort(new Comparator<Diagnostic>() {

			@Override
			public int compare(Diagnostic d1, Diagnostic d2) {
				return d1.getRange().getStart().getCharacter() - d2.getRange().getStart().getCharacter();
			}
		});
		Diagnostic diagnostic1 = diagnostics.get(0);
		Range range1 = diagnostic1.getRange();
		checkRange(range1, lineNumber, 18, lineNumber, 38);
		assertThat(diagnostic1.getMessage()).isEqualTo("More than one trait defines the same property: quarkus.enabled");
		Diagnostic diagnostic2 = diagnostics.get(1);
		Range range2 = diagnostic2.getRange();
		checkRange(range2, lineNumber, 45, lineNumber, 66);
		assertThat(diagnostic2.getMessage()).isEqualTo("More than one trait defines the same property: quarkus.enabled");
	}
	
}
