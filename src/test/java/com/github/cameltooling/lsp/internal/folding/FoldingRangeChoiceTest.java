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
package com.github.cameltooling.lsp.internal.folding;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.eclipse.lsp4j.FoldingRange;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class FoldingRangeChoiceTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void testFoldingRangeWithoutEnd() throws Exception {
		File file = new File("src/test/resources/workspace/RouteWithChoice.java");
		CamelLanguageServer languageServer = initializeLanguageServer(file);
		
		List<FoldingRange> foldingRanges = getFoldingRanges(file, languageServer).get();
		
		assertThat(foldingRanges).hasSize(2);
		FoldingRange foldingRange = foldingRanges.get(1);
		assertThat(foldingRange).isEqualTo(new FoldingRange(6, 12));
	}
	
	@Test
	void testFoldingRangeWithSeveralRoutes() throws Exception {
		File file = new File("src/test/resources/workspace/RoutesWithChoice.java");
		CamelLanguageServer languageServer = initializeLanguageServer(file);
		
		List<FoldingRange> foldingRanges = getFoldingRanges(file, languageServer).get();
		
		assertThat(foldingRanges).hasSize(4);
	}
	
	@Test
	void testFoldingRangeWithEnd() throws Exception {
		File file = new File("src/test/resources/workspace/RouteWithChoiceAndEnd.java");
		CamelLanguageServer languageServer = initializeLanguageServer(file);
		
		List<FoldingRange> foldingRanges = getFoldingRanges(file, languageServer).get();
		
		assertThat(foldingRanges).hasSize(2);
		FoldingRange foldingRange = foldingRanges.get(1);
		assertThat(foldingRange).isEqualTo(new FoldingRange(6, 13));
	}
	
}
