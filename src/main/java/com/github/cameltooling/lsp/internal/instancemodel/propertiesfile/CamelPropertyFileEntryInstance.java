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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

/**
 * Represents one entry in properties file. For instance, the whole entry "camel.component.timer.delay=1000"
 *
 */
public class CamelPropertyFileEntryInstance {
	
	private CamelPropertyFileKeyInstance camelPropertyFileKeyInstance;
	private CamelPropertyFileValueInstance camelPropertyFileValueInstance;

	public CamelPropertyFileEntryInstance(CompletableFuture<CamelCatalog> camelCatalog, String line, TextDocumentItem textDocumentItem) {
		int indexOf = line.indexOf('=');
		String camelPropertyFileKeyInstanceString;
		String camelPropertyFileValueInstanceString;
		if (indexOf != -1) {
			camelPropertyFileKeyInstanceString = line.substring(0, indexOf);
			camelPropertyFileValueInstanceString = line.substring(indexOf+1);
		} else {
			camelPropertyFileKeyInstanceString = line;
			camelPropertyFileValueInstanceString = null;
		}
		camelPropertyFileKeyInstance = new CamelPropertyFileKeyInstance(camelCatalog, camelPropertyFileKeyInstanceString, this);
		camelPropertyFileValueInstance = new CamelPropertyFileValueInstance(camelCatalog, camelPropertyFileValueInstanceString, camelPropertyFileKeyInstance, textDocumentItem);
	}
	
	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		if (position.getCharacter() <= camelPropertyFileKeyInstance.getEndposition()) {
			return camelPropertyFileKeyInstance.getCompletions(position);
		} else {
			return camelPropertyFileValueInstance.getCompletions(position);
		}
	}
	
	CamelPropertyFileKeyInstance getCamelPropertyFileKeyInstance() {
		return camelPropertyFileKeyInstance;
	}

	CamelPropertyFileValueInstance getCamelPropertyFileValueInstance() {
		return camelPropertyFileValueInstance;
	}
}
