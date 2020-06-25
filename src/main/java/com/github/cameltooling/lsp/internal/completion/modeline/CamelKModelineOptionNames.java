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
package com.github.cameltooling.lsp.internal.completion.modeline;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.CompletionItem;

public class CamelKModelineOptionNames {
	
	private CamelKModelineOptionNames() {}

	private static final List<CompletionItem> COMPLETION_ITEMS;
	
	static {
		COMPLETION_ITEMS = new ArrayList<>();
		COMPLETION_ITEMS.add(createCompletionItem("dependency", "An external library that should be included. E.g. for Maven dependencies \"dependency=mvn:org.my/app:1.0\""));
		COMPLETION_ITEMS.add(createCompletionItem("env", "Set an environment variable in the integration container. E.g \"env=MY_VAR=my-value\""));
		COMPLETION_ITEMS.add(createCompletionItem("label", "Add a label to the integration. E.g. \"label=my.company=hello\""));
		COMPLETION_ITEMS.add(createCompletionItem("name", "The integration name"));
		COMPLETION_ITEMS.add(createCompletionItem("open-api", "Add an OpenAPI v2 spec (file path)"));
		COMPLETION_ITEMS.add(createCompletionItem("profile", "Trait profile used for deployment"));
		COMPLETION_ITEMS.add(createCompletionItem("property", "Add a camel property"));
		COMPLETION_ITEMS.add(createCompletionItem("property-file", "Bind a property file to the integration. E.g. \"property-file=integration.properties\""));
		COMPLETION_ITEMS.add(createCompletionItem("resource", "Add a resource"));
		COMPLETION_ITEMS.add(createCompletionItem("trait", "Configure a trait. E.g. \"trait=service.enabled=false\""));
	}

	private static CompletionItem createCompletionItem(String label, String documentation) {
		CompletionItem completionItem = new CompletionItem(label);
		completionItem.setDocumentation(documentation);
		return completionItem;
	}
	
	public static List<CompletionItem> getCompletionItems() {
		return COMPLETION_ITEMS;
	}
	
}
