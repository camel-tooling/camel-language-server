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
package com.github.cameltooling.lsp.internal.catalog.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.camel.catalog.DefaultCamelCatalog;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;

class ModelHelperTest {

	@Test
	void testGenerateComponentModelForTimerWithoutError() throws Exception {
		ComponentModel componentModel = ModelHelper.generateComponentModel(new DefaultCamelCatalog().componentJSonSchema("timer"), true);
		assertThat(componentModel).isNotNull();
	}

	@Test
	void testCamelComponentWithoutComponentProperties() throws Exception {
		ComponentModel componentModel = ModelHelper.generateComponentModel(
				"{\"component\": {\"kind\": \"component\", \"scheme\": \"acomponent\", \"syntax\": \"acomponent:withsyntax\"}}",
				true);
		assertThat(componentModel).isNotNull();
	}

	@Test
	void testCanLoadAllComponentsFromCurrentCamelCatalog() throws Exception {
		List<String> componentNames = new DefaultCamelCatalog().findComponentNames();
		for (String componentName : componentNames) {
			String componentJSonSchema = new DefaultCamelCatalog().componentJSonSchema(componentName);
			ComponentModel componentModel = ModelHelper.generateComponentModel(componentJSonSchema, true);
			assertThat(componentModel)
				.as("Cannot load component %s which has the following schema\n %s", componentName,componentJSonSchema)
				.isNotNull();
		}
	}

}
