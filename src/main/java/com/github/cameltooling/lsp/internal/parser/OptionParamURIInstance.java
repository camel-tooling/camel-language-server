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
package com.github.cameltooling.lsp.internal.parser;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

public class OptionParamURIInstance extends CamelUriElementInstance {
	
	private OptionParamKeyURIInstance key;
	private OptionParamValueURIInstance value;
	private CamelURIInstance camelURIInstance;

	public OptionParamURIInstance(CamelURIInstance camelURIInstance, String optionParam, int startPosition, int endPosition) {
		super(startPosition, endPosition);
		this.camelURIInstance = camelURIInstance;
		String[] splittedParams = optionParam.split("=");
		String keyName = splittedParams[0];
		key = new OptionParamKeyURIInstance(this, splittedParams[0], startPosition, startPosition + keyName.length());
		if (splittedParams.length > 1) {
			value = new OptionParamValueURIInstance(this, splittedParams[1], startPosition + keyName.length() + 1, endPosition);
		} else if(optionParam.endsWith("=")){
			value = new OptionParamValueURIInstance(this, null, startPosition + keyName.length() + 1, endPosition);
		}
	}

	public OptionParamKeyURIInstance getKey() {
		return key;
	}

	public OptionParamValueURIInstance getValue() {
		return value;
	}

	public CamelUriElementInstance getSpecificElement(int position) {
		if(key.isInRange(position)) {
			return key;
		} else {
			return value;
		}
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	public String getComponentName() {
		return camelURIInstance.getComponent().getComponentName();
	}
	
	public boolean isProducer() {
		return camelURIInstance.isProducer();
	}
	
}
