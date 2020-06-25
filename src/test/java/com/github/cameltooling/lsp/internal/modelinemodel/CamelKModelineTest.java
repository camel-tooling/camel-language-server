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
package com.github.cameltooling.lsp.internal.modelinemodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CamelKModelineTest {
	
	String modelineString = "// camel-k: language=groovy";
	CamelKModeline basicModeline = new CamelKModeline(modelineString);
	
	public static Stream<Arguments> data() {
		return Stream.of(
				Arguments.of("// camel-k: language=groovy"),
				Arguments.of("# camel-k: language=yaml"),
				Arguments.of("<!-- camel-k: language=xml -->"));
	}
	
	@ParameterizedTest
	@MethodSource("data")
	void testGetLine(String modelineString) throws Exception {
		CamelKModeline modeline = new CamelKModeline(modelineString);
		assertThat(modeline.getLine()).isEqualTo(0);
	}

	@ParameterizedTest
	@MethodSource("data")
	void testGetStartPositionInLine(String modelineString) throws Exception {
		CamelKModeline modeline = new CamelKModeline(modelineString);
		assertThat(modeline.getStartPositionInLine()).isEqualTo(0);
	}

	@ParameterizedTest
	@MethodSource("data")
	void testGetEndPositionInLine(String modelineString) throws Exception {
		CamelKModeline modeline = new CamelKModeline(modelineString);
		assertThat(modeline.getEndPositionInLine()).isEqualTo(modelineString.length());
	}
	
	@ParameterizedTest
	@MethodSource("data")
	void testGetNumberOfOptions(String modelineString) throws Exception {
		CamelKModeline modeline = new CamelKModeline(modelineString);
		assertThat(modeline.getOptions()).hasSize(1);
	}

}
