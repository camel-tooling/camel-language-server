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
package com.github.cameltooling.lsp.internal.instancemodel.propertiesfile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.completion.CamelComponentOptionValuesCompletionsFuture;
import com.github.cameltooling.lsp.internal.completion.CamelEndpointCompletionProcessor;
import com.github.cameltooling.lsp.internal.parser.CamelKafkaUtil;

/**
 * Represents one value in properties file.
 * For instance, with "camel.component.timer.delay=1000",
 * it is used to represents "1000"
 */
public class CamelPropertyFileValueInstance {

	private CompletableFuture<CamelCatalog> camelCatalog;
	private String camelPropertyFileValue;
	private CamelPropertyFileKeyInstance key;

	private TextDocumentItem textDocumentItem;

	public CamelPropertyFileValueInstance(CompletableFuture<CamelCatalog> camelCatalog, String camelPropertyFileValue, CamelPropertyFileKeyInstance key, TextDocumentItem textDocumentItem) {
		this.camelCatalog = camelCatalog;
		this.camelPropertyFileValue = camelPropertyFileValue;
		this.key = key;
		this.textDocumentItem = textDocumentItem;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		if (new CamelKafkaUtil().isCamelURIForKafka(key.getCamelPropertyFileKey())) {
			return new CamelEndpointCompletionProcessor(textDocumentItem, camelCatalog).getCompletions(position);
		} else {
			String startFilter = camelPropertyFileValue.substring(0, position.getCharacter() - key.getEndposition() -1);
			return camelCatalog.thenApply(new CamelComponentOptionValuesCompletionsFuture(this, startFilter));
		}
	}

	public String getCamelPropertyFileValue() {
		return camelPropertyFileValue;
	}
	
	public CamelPropertyFileKeyInstance getKey() {
		return key;
	}

}
