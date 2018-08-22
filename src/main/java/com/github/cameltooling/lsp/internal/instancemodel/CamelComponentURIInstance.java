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
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.completion.CamelComponentSchemesCompletionsFuture;
import com.github.cameltooling.model.ComponentModel;

/**
 * For a Camel URI "timer:timerName?delay=10s", it represents "timer"
 *
 */
public class CamelComponentURIInstance extends CamelUriElementInstance {
	
	private CamelComponentAndPathUriInstance parent;
	private String componentName;

	public CamelComponentURIInstance(CamelComponentAndPathUriInstance parent, String componentName, int endPosition) {
		super(0, endPosition);
		this.parent = parent;
		this.componentName = componentName;
	}

	public String getComponentName() {
		return componentName;
	}

	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri) {
		if(getStartPositionInUri() <= positionInCamelUri && positionInCamelUri <= getEndPositionInUri()) {
			return camelCatalog.thenApply(new CamelComponentSchemesCompletionsFuture(getFilter(positionInCamelUri)));
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}		
	}
	
	/**
	 * returns the filter string to be applied on the list of all completions
	 * 
	 * @param positionInUri	the position
	 * @return	the filter string or null if not to be filtered
	 */
	private String getFilter(int positionInUri) { 
		if (componentName != null && componentName.trim().length()>0 && getStartPositionInUri() != positionInUri) {
			return componentName.substring(getStartPositionInUri(), positionInUri);
		}
		return null;
	}
	
	@Override
	public String getDescription(ComponentModel componentModel) {
		return componentModel.getDescription();
	}
	
	@Override
	public CamelURIInstance getCamelUriInstance() {
		return parent.getCamelUriInstance();
	}
}
