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

import org.eclipse.lsp4j.CompletionItem;

public class TraitProperty {

	private String name;
	private String description;
	private String type;
	private Object defaultValue;
	
	public CompletionItem createCompletionItem() {
		CompletionItem completionItem = new CompletionItem(name);
		completionItem.setDocumentation(description);
		if (defaultValue != null) {
			if("int".equals(type) && defaultValue instanceof Double) {
				completionItem.setInsertText(name+"="+((Double)defaultValue).intValue());
			} else {
				completionItem.setInsertText(name+"="+defaultValue);
			}
		}
		return completionItem;
	}
	
}
