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

import java.util.List;

import org.junit.jupiter.api.Test;

class CamelKModelineTraitOptionTest {
	
	@Test
	void checkDefinitionName() throws Exception {
		CamelKModelineTraitOption traitOption = retrieveFirstTrait("// camel-k: trait=quarkus.enabled=true");
		assertThat(traitOption.getTraitDefinitionName()).isEqualTo("quarkus");
	}
	
	@Test
	void checkDefinitionNameWithIncompleteValues() throws Exception {
		CamelKModelineTraitOption traitOption = retrieveFirstTrait("// camel-k: trait=quarkus.");
		assertThat(traitOption.getTraitDefinitionName()).isEqualTo("quarkus");
	}
	
	@Test
	void checkPropertyName() throws Exception {
		CamelKModelineTraitOption traitOption = retrieveFirstTrait("// camel-k: trait=quarkus.enabled=true");
		assertThat(traitOption.getTraitPropertyName()).isEqualTo("enabled");
	}
	
	@Test
	void checkPropertyNameWithoutValue() throws Exception {
		CamelKModelineTraitOption traitOption = retrieveFirstTrait("// camel-k: trait=quarkus.enabled=");
		assertThat(traitOption.getTraitPropertyName()).isEqualTo("enabled");
	}

	private CamelKModelineTraitOption retrieveFirstTrait(String modelineWithMixedSeparator) {
		List<CamelKModelineOption> options = new CamelKModeline(modelineWithMixedSeparator).getOptions();
		assertThat(options).hasSize(1);
		return (CamelKModelineTraitOption) options.get(0).getOptionValue();
	}

}
