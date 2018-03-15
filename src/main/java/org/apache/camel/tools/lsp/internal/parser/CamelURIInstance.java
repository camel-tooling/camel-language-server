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
package org.apache.camel.tools.lsp.internal.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tools.lsp.internal.completion.CamelComponentSchemaCompletionsFuture;
import org.eclipse.lsp4j.CompletionItem;
import org.w3c.dom.Node;

public class CamelURIInstance extends CamelUriElementInstance {
	
	private static final String CAMEL_PATH_SEPARATOR_REGEX = ":|/";
	private static final String PARAMETERS_SEPARATOR = "&amp;";
	private static final List<String> PRODUCER_NODE_TAG = Arrays.asList("to", "interceptSendToEndpoint", "wireTap", "deadLetterChanel");
	private CamelComponentURIInstance component;
	private Set<PathParamURIInstance> pathParams = new HashSet<>();
	private Set<OptionParamURIInstance> optionParams = new HashSet<>();
	private Node node;
	
	public CamelURIInstance(String uriToParse) {
		this(uriToParse, null);
	}
	
	public CamelURIInstance(String uriToParse, Node node) {
		super(0, uriToParse != null ? uriToParse.length() : 0);
		this.node = node;
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
			String[] allOptionParams = uriToParse.substring(posEndofPathParams + 1).split(PARAMETERS_SEPARATOR);
			int currentPosition = posEndofPathParams + 1;
			for (String optionParam : allOptionParams) {
				optionParams.add(new OptionParamURIInstance(this, optionParam, currentPosition, currentPosition + optionParam.length()));
				currentPosition += optionParam.length() + 5;
			}
			if(uriToParse.endsWith(PARAMETERS_SEPARATOR)) {
				optionParams.add(new OptionParamURIInstance(this, "", currentPosition, currentPosition));
			}
		}
	}

	private void initPathParams(String uriToParse, int posDoubleDot, int posEndofPathParams) {
		String[] allPathParams = uriToParse.substring(posDoubleDot + 1, posEndofPathParams).split(CAMEL_PATH_SEPARATOR_REGEX);
		int currentPosition = posDoubleDot + 1;
		for (String pathParam : allPathParams) {
			pathParams.add(new PathParamURIInstance(pathParam, currentPosition, currentPosition+pathParam.length()));
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
			return camelCatalog.thenApply(new CamelComponentSchemaCompletionsFuture());
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
	}
	
	public boolean isProducer() {
		return PRODUCER_NODE_TAG.contains(node.getNodeName());
	}
	
}
