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
package com.github.cameltooling.lsp.internal.completion.traits;

import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitDefinitionProperty;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitOption;

public class TraitProperty {

	private String name;
	private String description;
	private String type;
	private Object defaultValue;
	
	public CompletionItem createCompletionItem(CamelKModelineTraitDefinitionProperty camelKModelineTraitDefinitionProperty) {
		CompletionItem completionItem = new CompletionItem(name);
		completionItem.setDocumentation(description);
		CamelKModelineTraitOption traitOption = camelKModelineTraitDefinitionProperty.getTraitOption();
		if (hasAValueSpecified(traitOption)) {
			completionItem.setInsertText(name);
		} else {
			String prefix = name + "=";
			if (defaultValue != null) {
				if ("int".equals(type) && defaultValue instanceof Double) {
					completionItem.setInsertText(prefix + ((Double) defaultValue).intValue());
				} else {
					completionItem.setInsertText(prefix + defaultValue);
				}
			} else {
				completionItem.setInsertText(prefix);
			}
		}
		CompletionResolverUtils.applyTextEditToCompletionItem(camelKModelineTraitDefinitionProperty, completionItem);
		return completionItem;
	}

	private boolean hasAValueSpecified(CamelKModelineTraitOption traitOption) {
		return traitOption.getValueAsString().contains("=");
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}
	
}
