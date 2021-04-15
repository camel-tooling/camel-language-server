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
package com.github.cameltooling.lsp.internal.instancemodel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.w3c.dom.Node;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.completion.CamelComponentSchemesCompletionsFuture;
import com.github.cameltooling.lsp.internal.settings.SettingsManager;

/**
 * represents the whole Camel URI
 * this class will provide code completions if the uri is empty
 * 
 * @author apupier
 */
public class CamelURIInstance extends CamelUriElementInstance {
	
	private static final List<String> PRODUCER_TYPE_POSSIBLE_NAMES = Arrays.asList("to", "interceptSendToEndpoint", "wireTap", "deadLetterChanel");
	
	private DSLModelHelper dslModelHelper;
	
	private Position startPositionInDocument;
	private Position endPositionInDocument;
	
	private CamelComponentAndPathUriInstance componentAndPathUriElementInstance;
	private Set<OptionParamURIInstance> optionParams = new HashSet<>();
		
	public CamelURIInstance(String uriToParse, Node node, TextDocumentItem textDocumentItem) {
		super(0, uriToParse != null ? uriToParse.length() : 0);
		setDocument(textDocumentItem);
		dslModelHelper = new XMLDSLModelHelper(node);
		init(uriToParse);
	}
	
	public CamelURIInstance(String uriToParse, String methodName, TextDocumentItem textDocumentItem) {
		super(0, uriToParse != null ? uriToParse.length() : 0);
		setDocument(textDocumentItem);
		dslModelHelper = new JavaDSLModelHelper(methodName);
		init(uriToParse);
	}
	
	public CamelURIInstance(String uriToParse, DSLModelHelper dslModelHelper, TextDocumentItem textDocumentItem) {
		super(0, uriToParse != null ? uriToParse.length() : 0);
		setDocument(textDocumentItem);
		this.dslModelHelper = dslModelHelper;
		init(uriToParse);
	}
	
	private void init(String uriToParse) {
		if(uriToParse != null && !uriToParse.isEmpty()) {
			int posQuestionMark = uriToParse.indexOf('?');
			if (posQuestionMark > 0) {
				componentAndPathUriElementInstance = new CamelComponentAndPathUriInstance(this, uriToParse.substring(0, posQuestionMark), posQuestionMark);
			} else {
				componentAndPathUriElementInstance = new CamelComponentAndPathUriInstance(this, uriToParse, uriToParse.length());
			}
			initOptionParams(uriToParse, posQuestionMark > 0 ? posQuestionMark : uriToParse.length());
		}
	}

	private void initOptionParams(String uriToParse, int posEndofPathParams) {
		if(uriToParse.length() > posEndofPathParams) {
			String parametersSeparator = dslModelHelper.getParametersSeparator();
			String[] allOptionParams = uriToParse.substring(posEndofPathParams + 1).split(parametersSeparator);
			int currentPosition = posEndofPathParams + 1;
			for (String optionParam : allOptionParams) {
				optionParams.add(new OptionParamURIInstance(this, optionParam, currentPosition, currentPosition + optionParam.length()));
				currentPosition += optionParam.length() + parametersSeparator.length();
			}
			if(uriToParse.endsWith(parametersSeparator)) {
				optionParams.add(new OptionParamURIInstance(this, "", currentPosition, currentPosition));
			}
		}
	}

	public Set<OptionParamURIInstance> getOptionParams() {
		return optionParams;
	}

	public CamelUriElementInstance getSpecificElement(int position) {
		if (componentAndPathUriElementInstance != null && componentAndPathUriElementInstance.isInRange(position)) {
			return componentAndPathUriElementInstance.getSpecificElement(position);
		} else {
			for (OptionParamURIInstance optionParamURIInstance : optionParams) {
				if (optionParamURIInstance.isInRange(position)) {
					return optionParamURIInstance.getSpecificElement(position);
				}
			}
		}
		return this;
	}

	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri, TextDocumentItem docItem, SettingsManager settingsManager) {
		if(getStartPositionInUri() <= positionInCamelUri && positionInCamelUri <= getEndPositionInUri()) {
			return camelCatalog.thenApply(new CamelComponentSchemesCompletionsFuture(this, getFilter(), docItem));
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
	}
	
	/**
	 * returns the filter string to be applied on the list of all completions
	 * 
	 * @return	the filter string or null if not to be filtered
	 */
	private String getFilter() { 
		if (componentAndPathUriElementInstance != null) {
			return String.format("%s:", componentAndPathUriElementInstance.getComponentName());
		}
		return null;
	}
	
	public boolean isProducer() {
		return PRODUCER_TYPE_POSSIBLE_NAMES.contains(dslModelHelper.getTypeDeterminingProducerConsumer());
	}
	
	/**
	 * @return the componentAndPathUriElementInstance
	 */
	public CamelComponentAndPathUriInstance getComponentAndPathUriElementInstance() {
		return this.componentAndPathUriElementInstance;
	}
	
	@Override
	public String getComponentName() {
		if (componentAndPathUriElementInstance != null) {
			return componentAndPathUriElementInstance.getComponentName();
		} else {
			return null;
		}
	}
	
	@Override
	public String getDescription(ComponentModel componentModel) {
		return null;
	}
	
	@Override
	public CamelURIInstance getCamelUriInstance() {
		return this;
	}
	
	public Position getStartPositionInDocument() {
		return startPositionInDocument;
	}

	public void setStartPositionInDocument(Position positionInDocument) {
		this.startPositionInDocument = positionInDocument;
	}
	
	public Position getEndPositionInDocument() {
		return this.endPositionInDocument;
	}
	
	public void setEndPositionInDocument(Position endPositionInDocument) {
		this.endPositionInDocument = endPositionInDocument;
	}
	
	public Range getAbsoluteBounds() {
		return new Range(startPositionInDocument, endPositionInDocument);
	}

}
