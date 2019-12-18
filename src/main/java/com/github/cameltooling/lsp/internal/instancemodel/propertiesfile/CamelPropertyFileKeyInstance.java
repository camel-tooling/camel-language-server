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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

/**
 * Represents one key in properties file.
 * For instance, with "camel.component.timer.delay=1000",
 * it is used to represents "camel.component.timer.delay"
 * 
 */
public class CamelPropertyFileKeyInstance {
	
	private static final String CAMEL_KEY_PREFIX = "camel.";
	static final String CAMEL_COMPONENT_KEY_PREFIX = "camel.component.";
	
	private String camelPropertyFileKey;
	private CamelComponentPropertyFilekey camelComponentPropertyFilekey;

	public CamelPropertyFileKeyInstance(CompletableFuture<CamelCatalog> camelCatalog, String camelPropertyFileKey) {
		this.camelPropertyFileKey = camelPropertyFileKey;
		if (camelPropertyFileKey.startsWith(CAMEL_COMPONENT_KEY_PREFIX)) {
			camelComponentPropertyFilekey = new CamelComponentPropertyFilekey(camelCatalog, camelPropertyFileKey.substring(CAMEL_COMPONENT_KEY_PREFIX.length()));
		}
	}

	public int getEndposition() {
		return camelPropertyFileKey.length();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(int positionChar) {
		if (CAMEL_KEY_PREFIX.length() == positionChar && camelPropertyFileKey.startsWith(CAMEL_KEY_PREFIX)) {
			return getTopLevelCamelCompletion();
		} else if(camelComponentPropertyFilekey != null && camelComponentPropertyFilekey.isInRange(positionChar)) {
			return camelComponentPropertyFilekey.getCompletions(positionChar);
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}
	

	protected CompletableFuture<List<CompletionItem>> getTopLevelCamelCompletion() {
		List<CompletionItem> completions = new ArrayList<>();
		completions.add(new CompletionItem("component"));
		completions.add(new CompletionItem("main"));
		completions.add(new CompletionItem("rest"));
		completions.add(new CompletionItem("hystrix"));
		return CompletableFuture.completedFuture(completions);
	}

}
