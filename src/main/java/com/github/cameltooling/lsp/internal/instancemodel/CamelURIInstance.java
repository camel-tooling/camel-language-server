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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.w3c.dom.Node;

import com.github.cameltooling.lsp.internal.completion.CamelComponentSchemesCompletionsFuture;
import com.github.cameltooling.model.ComponentModel;

/**
 * represents the whole Camel URI
 * this class will provide code completions if the uri is empty
 * 
 * @author apupier
 */
public class CamelURIInstance extends CamelUriElementInstance {
	
	private static final String CAMEL_PATH_SEPARATOR_REGEX = ":|/";
	private static final List<String> PRODUCER_TYPE_POSSIBLE_NAMES = Arrays.asList("to", "interceptSendToEndpoint", "wireTap", "deadLetterChanel");
	private CamelComponentURIInstance component;
	private Set<PathParamURIInstance> pathParams = new HashSet<>();
	private Set<OptionParamURIInstance> optionParams = new HashSet<>();
	private DSLModelHelper dslModelHelper;
	
	public CamelURIInstance(String uriToParse, Node node) {
		super(0, uriToParse != null ? uriToParse.length() : 0);
		dslModelHelper = new XMLDSLModelHelper(node);
		init(uriToParse);
	}
	
	/**
	 * @param uriToParse
	 * @param methodName the method name of the Java call encapsulating the provided uri to parse
	 */
	public CamelURIInstance(String uriToParse, String methodName) {
		super(0, uriToParse != null ? uriToParse.length() : 0);
		dslModelHelper = new JavaDSLModelHelper(methodName);
		init(uriToParse);
	}
	
	private void init(String uriToParse) {
		if(uriToParse != null && !uriToParse.isEmpty()) {
			int posDoubleDot = uriToParse.indexOf(':');
			if (posDoubleDot > 0) {
				component = new CamelComponentURIInstance(uriToParse.substring(0, posDoubleDot), posDoubleDot);
				int posEndofPathParams = getPosEndOfPathParams(posDoubleDot, uriToParse);
				initPathParams(uriToParse, posDoubleDot, posEndofPathParams);
				initOptionParams(uriToParse, posEndofPathParams);
			} else {
				component = new CamelComponentURIInstance(uriToParse, uriToParse.length());
			}
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

	private void initPathParams(String uriToParse, int posDoubleDot, int posEndofPathParams) {
		String[] allPathParams = uriToParse.substring(posDoubleDot + 1, posEndofPathParams).split(CAMEL_PATH_SEPARATOR_REGEX);
		int currentPosition = posDoubleDot + 1;
		for (String pathParam : allPathParams) {
			pathParams.add(new PathParamURIInstance(this, pathParam, currentPosition, currentPosition+pathParam.length()));
			currentPosition += pathParam.length() + 1;
		}
	}

	private int getPosEndOfPathParams(int posDoubleDot, String uriToParse) {
		int questionMarkPosition = uriToParse.indexOf('?', posDoubleDot);
		if (questionMarkPosition > 0) {
			return questionMarkPosition;
		} else {
			return uriToParse.length();
		}
	}

	public CamelComponentURIInstance getComponent() {
		return component;
	}

	public Set<PathParamURIInstance> getPathParams() {
		return pathParams;
	}

	public Set<OptionParamURIInstance> getOptionParams() {
		return optionParams;
	}

	public CamelUriElementInstance getSpecificElement(int position) {
		if (component != null && component.isInRange(position)) {
			return component;
		} else {
			for (PathParamURIInstance pathParamURIInstance : pathParams) {
				if(pathParamURIInstance.isInRange(position)) {
					return pathParamURIInstance;
				}
			}
			for (OptionParamURIInstance optionParamURIInstance : optionParams) {
				if (optionParamURIInstance.isInRange(position)) {
					return optionParamURIInstance.getSpecificElement(position);
				}
			}
		}
		return this;
	}

	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri) {
		if(getStartPosition() <= positionInCamelUri && positionInCamelUri <= getEndPosition()) {
			return camelCatalog.thenApply(new CamelComponentSchemesCompletionsFuture(getFilter()));
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
		if (component != null) {
			return String.format("%s:", component.getComponentName());
		}
		return null;
	}
	
	public boolean isProducer() {
		return PRODUCER_TYPE_POSSIBLE_NAMES.contains(dslModelHelper.getTypeDeterminingProducerConsumer());
	}
	
	@Override
	public String getComponentName() {
		return component.getComponentName();
	}
	
	@Override
	public String getDescription(ComponentModel componentModel) {
		return null;
	}
}
