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

class CamelKModelineOptionTest {
	
	String modelineString = "// camel-k: language=groovy";
	CamelKModeline basicModeline = new CamelKModeline(modelineString, null, 0);

	@Test
	void testOptionParsingBasic() throws Exception {
		List<CamelKModelineOption> options = basicModeline.getOptions();
		assertThat(options).hasSize(1);
		CamelKModelineOption languageOption = options.get(0);
		assertThat(languageOption.getOptionName()).isEqualTo("language");
		assertThat(languageOption.getOptionValue().getValueAsString()).isEqualTo("groovy");
	}
	
	@Test
	void testOptionTraitWithIncompleteTraitContent() throws Exception {
		String modelineWithMixedSeparator = "// camel-k: language=groovy trait=quarkus";
		List<CamelKModelineOption> options = new CamelKModeline(modelineWithMixedSeparator, null, 0).getOptions();
		assertThat(options).hasSize(2);
	}
	
	@Test
	void test2Options() throws Exception {
		String modelineWith2Options = "// camel-k: language=groovy trait=quarkus.enabled=true";
		checkFor2Options(modelineWith2Options);
	}
	
	@Test
	void test2OptionsWithTabSeparator() throws Exception {
		String modelineWith2Options = "// camel-k:\tlanguage=groovy\ttrait=quarkus.enabled=true";
		checkFor2Options(modelineWith2Options);
	}
	
	@Test
	void testOptionsWithMixedSeparator() throws Exception {
		String modelineWithMixedSeparator = "// camel-k:\tlanguage=groovy trait=quarkus.enabled=true\tdependency=mvn:org.my/application:1.0";
		List<CamelKModelineOption> options = new CamelKModeline(modelineWithMixedSeparator, null, 0).getOptions();
		assertThat(options).hasSize(3);
	}
	
	@Test
	void testOptionsWithIncompleteOptions() throws Exception {
		String modelineWithMixedSeparator = "// camel-k: language=groovy trait";
		List<CamelKModelineOption> options = new CamelKModeline(modelineWithMixedSeparator, null, 0).getOptions();
		assertThat(options).hasSize(2);
		CamelKModelineOption incompleteOption = options.get(1);
		assertThat(incompleteOption.getOptionName()).isEqualTo("trait");
		assertThat(incompleteOption.getOptionValue()).isNull();
		assertThat(incompleteOption.getStartPositionInLine()).isEqualTo(28);
		assertThat(incompleteOption.getEndPositionInLine()).isEqualTo(33);
	}

	private void checkFor2Options(String modelineWith2Options) {
		List<CamelKModelineOption> options = new CamelKModeline(modelineWith2Options, null, 0).getOptions();
		assertThat(options).hasSize(2);
		CamelKModelineOption languageGroovyOption = options.get(0);
		assertThat(languageGroovyOption.getOptionName()).isEqualTo("language");
		assertThat(languageGroovyOption.getOptionValue().getValueAsString()).isEqualTo("groovy");
		assertThat(languageGroovyOption.getStartPositionInLine()).isEqualTo(12);
		assertThat(languageGroovyOption.getEndPositionInLine()).isEqualTo(27);
		CamelKModelineOption traitOption = options.get(1);
		assertThat(traitOption.getOptionName()).isEqualTo("trait");
		assertThat(traitOption.getOptionValue().getValueAsString()).isEqualTo("quarkus.enabled=true");
		assertThat(traitOption.getStartPositionInLine()).isEqualTo(28);
		assertThat(traitOption.getEndPositionInLine()).isEqualTo(modelineWith2Options.length());
	}
	
}
