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

import java.util.List;

import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitDefinition;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitOption;

public class TraitDefinition {
	
	private String name;
	private String description;
	private boolean platform;
	private List<String> profiles;
	private List<TraitProperty> properties;
	
	public CompletionItem createCompletionItem(CamelKModelineTraitDefinition traitDefinition) {
		CompletionItem completionItem = new CompletionItem(name);
		completionItem.setDocumentation(description);
		if(hasAPropertySpecified(traitDefinition.getTraitOption())) {
			completionItem.setInsertText(name);
		} else {
			completionItem.setInsertText(name + ".");
		}
		CompletionResolverUtils.applyTextEditToCompletionItem(traitDefinition, completionItem);
		return completionItem;
	}

	public List<TraitProperty> getProperties() {
		return properties;
	}

	public String getDescription() {
		return description;
	}

	public String getName() {
		return name;
	}

	public boolean isPlatform() {
		return platform;
	}

	public List<String> getProfiles() {
		return profiles;
	}
	
	private boolean hasAPropertySpecified(CamelKModelineTraitOption traitOption) {
		return traitOption.getTraitProperty() != null;
	}

}
