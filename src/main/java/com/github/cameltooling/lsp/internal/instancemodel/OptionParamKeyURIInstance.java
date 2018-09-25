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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.completion.CamelOptionNamesCompletionsFuture;
import com.github.cameltooling.model.ComponentModel;

/**
 * For a Camel URI "timer:timerName?delay=10s", it represents "delay"
 *
 */
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
	
	public OptionParamURIInstance getOptionParamURIInstance() {
		return this.optionParamURIInstance;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri, TextDocumentItem docItem) {
		if(getStartPositionInUri() <= positionInCamelUri && positionInCamelUri <= getEndPositionInUri()) {
			return camelCatalog.thenApply(new CamelOptionNamesCompletionsFuture(this, getComponentName(), optionParamURIInstance.isProducer(), getFilter(positionInCamelUri), positionInCamelUri, getAlreadyDefinedUriOptions()));
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
	}
	
	public String getComponentName() {
		return optionParamURIInstance.getComponentName();
	}
	
	/**
	 * returns the filter string to be applied on the list of all completions
	 * 
	 * @param position the positionInUri
	 * @return	the filter string or null if not to be filtered
	 */
	private String getFilter(int positionInUri) {
		int len = positionInUri-getStartPositionInUri()-1;
		if (keyName != null && keyName.trim().length()>0 && getStartPositionInUri() != positionInUri) {
			return keyName.length()>len ? keyName.substring(0, Math.max(1, len)) : keyName;
		}
		return null;
	}
	
	private Set<OptionParamURIInstance> getAlreadyDefinedUriOptions() {
		return optionParamURIInstance.getCamelUriInstance().getOptionParams();
	}
	
	@Override
	public String getDescription(ComponentModel componentModel) {
		return optionParamURIInstance.getDescription(componentModel);
	}
	
	@Override
	public CamelURIInstance getCamelUriInstance() {
		return optionParamURIInstance.getCamelUriInstance();
	}
}
