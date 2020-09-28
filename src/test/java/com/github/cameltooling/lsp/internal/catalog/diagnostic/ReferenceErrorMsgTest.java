/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.catalog.diagnostic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReferenceErrorMsgTest {

	@Test
	void testGetErrorMessageNotStartingWithSharp() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("aKey", "aWrongReferenceNotStartingWith#");
		assertThat(new ReferenceErrorMsg().getErrorMessage(map.entrySet().iterator().next()))
			.contains("Invalid")
			.contains("must start with #")
			.contains("aWrongReferenceNotStartingWith#");
	}

	@Test
	void testGetErrorMessageStartingWithSharp() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("aKey", "#aWrongReferenceStartingWith");
		assertThat(new ReferenceErrorMsg().getErrorMessage(map.entrySet().iterator().next()))
			.contains("Invalid")
			.doesNotContain("must start with #").contains("#aWrongReferenceStartingWith");
	}

	@Test
	void testGetErrorMessageSpecifyEmpty() throws Exception {
		Map<String, String> map = new HashMap<>();
		map.put("aKey", "");
		assertThat(new ReferenceErrorMsg().getErrorMessage(map.entrySet().iterator().next()))
			.contains("Empty")
			.doesNotContain("must start with #");
	}

}
