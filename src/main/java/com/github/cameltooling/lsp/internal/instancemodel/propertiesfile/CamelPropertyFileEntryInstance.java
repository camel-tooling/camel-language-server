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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

/**
 * Represents one entry in properties file. For instance, the whole entry "camel.component.timer.delay=1000"
 *
 */
public class CamelPropertyFileEntryInstance {
	
	private CamelPropertyFileKeyInstance camelPropertyFileKeyInstance;

	public CamelPropertyFileEntryInstance(CompletableFuture<CamelCatalog> camelCatalog, String line) {
		int indexOf = line.indexOf('=');
		String camelPropertyFileKeyInstanceString;
		if (indexOf != -1) {
			camelPropertyFileKeyInstanceString = line.substring(0, indexOf);
		} else {
			camelPropertyFileKeyInstanceString = line;
		}
		camelPropertyFileKeyInstance = new CamelPropertyFileKeyInstance(camelCatalog, camelPropertyFileKeyInstanceString);
	}
	
	public CompletableFuture<List<CompletionItem>> getCompletions(int positionChar) {
		if (positionChar <= camelPropertyFileKeyInstance.getEndposition()) {
			return camelPropertyFileKeyInstance.getCompletions(positionChar);
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}
}
