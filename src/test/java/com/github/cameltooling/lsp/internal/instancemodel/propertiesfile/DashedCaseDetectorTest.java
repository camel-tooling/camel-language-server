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
package com.github.cameltooling.lsp.internal.instancemodel.propertiesfile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DashedCaseDetectorTest {

	@Test
	void testHasNoDashedCaseInCamelComponentOption() throws Exception {
		assertThat(hasDashed("camel.component.id.myNameNotDashed=123")).isFalse();
	}
	
	@Test
	void testHasNoDashedCaseInCamelComponentOptionWithSeveralLines() throws Exception {
		assertThat(hasDashed(
				"camel.component.id.myNameNotDashed=123\n" +
				"camel.component.id.myOtherNameNotDashed=123")).isFalse();
	}
	
	@Test
	void testIgnoreOtherLinesDashed() throws Exception {
		assertThat(hasDashed(
				"camel.component.id.myNameNotDashed=123\n" +
				"with-dashed")).isFalse();
	}

	@Test
	void testHasDashedCaseInCamelComponentOption() throws Exception {
		assertThat(hasDashed("camel.component.id.my-name-dashed=123")).isTrue();
	}
	
	@Test
	void testHasDashedCaseInCamelMainOption() throws Exception {
		assertThat(hasDashed("camel.main.auto-startup=123")).isTrue();
	}
	
	@Test
	void testHasDashedCaseInCamelComponentOptionWithSeveralLines() throws Exception {
		assertThat(hasDashed(
				"camel.component.id.myNameNotDashed=123\n" +
				"camel.component.id.my-name-dashed=123")).isTrue();
	}
	
	private boolean hasDashed(String text) {
		return new DashedCaseDetector().hasDashedCaseInCamelComponentOption(text);
	}
	
}
