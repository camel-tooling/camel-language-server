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
package com.github.cameltooling.lsp.internal.instancemodel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.completion.CamelOptionSchemaCompletionsFuture;

public class OptionParamKeyURIInstance extends CamelUriElementInstance {
	
	private String keyName;
	private OptionParamURIInstance optionParamURIInstance;

	public OptionParamKeyURIInstance(OptionParamURIInstance optionParamURIInstance, String keyName, int startPosition, int endPosition) {
		super(startPosition, endPosition);
		this.optionParamURIInstance = optionParamURIInstance;
		this.keyName = keyName;
	}

	public String getKeyName() {
		return keyName;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri) {
		return camelCatalog.thenApply(new CamelOptionSchemaCompletionsFuture(getComponentName(), optionParamURIInstance.isProducer()));
	}
	
	public String getComponentName() {
		return optionParamURIInstance.getComponentName();
	}

}
