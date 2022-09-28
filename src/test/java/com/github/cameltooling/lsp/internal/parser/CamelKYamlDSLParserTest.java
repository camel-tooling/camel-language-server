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
package com.github.cameltooling.lsp.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CamelKYamlDSLParserTest {

	@Test
	void testRepairEscapeCharacterWithNullLine() throws Exception {
		assertThat(new CamelYamlDSLParser().repairLostEscapeChars("\"", null)).isEmpty();
	}
	
	@Test
	void testRepairEscapeCharacterWithNoStringEncloser() throws Exception {
		assertThat(new CamelYamlDSLParser().repairLostEscapeChars(null, "myLine")).isEqualTo("myLine");
	}
	
	@Test
	void testRepairEscapeCharacterWithQuote() throws Exception {
		assertThat(new CamelYamlDSLParser().repairLostEscapeChars("'", "a value with ' quote inside")).isEqualTo("a value with '' quote inside");
	}
	
	@Test
	void testRepairEscapeCharacterWithDoubleQuote() throws Exception {
		assertThat(new CamelYamlDSLParser().repairLostEscapeChars("\"", "a value with double-quote \" inside")).isEqualTo("a value with double-quote \\\" inside");
	}
	
	@Test
	@Disabled("see https://github.com/camel-tooling/camel-language-server/issues/301")
	void testRepairEscapeCharacterWithDoubleQuoteAndSlash() throws Exception {
		assertThat(new CamelYamlDSLParser().repairLostEscapeChars("\"", "a value with backslash \\ inside")).isEqualTo("a value with double-quote \\\\ inside");
	}
}
