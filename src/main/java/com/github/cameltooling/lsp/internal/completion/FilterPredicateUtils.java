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
package com.github.cameltooling.lsp.internal.completion;

import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.catalog.model.EndpointOptionModel;
import com.github.cameltooling.lsp.internal.instancemodel.OptionParamURIInstance;

public class FilterPredicateUtils {

	private FilterPredicateUtils() {
		// util class
	}

	/**
	 * ensures that only completions are displayed which start with the text the user typed already
	 * 
	 * @param filterString	the filter string
	 * @return	the predicate
	 */
	public static Predicate<CompletionItem> matchesCompletionFilter(String filterString) {
		return item -> {
			if (filterString != null && filterString.trim().length() > 0) {
				return item.getLabel().startsWith(filterString)
						|| item.getInsertText() != null && item.getInsertText().startsWith(filterString);
			} else {
				return true;
			}
		};
	}

	/**
	 * ensures that only completions are displayed which start with the text the user typed already 
	 * 
	 * @param filterString	the filter string
	 * @return	the predicate
	 */
	public static Predicate<EndpointOptionModel> matchesEndpointOptionFilter(String filterString) {
		return item -> {
			if (filterString != null && filterString.trim().length()>0) {
				return item.getName().startsWith(filterString);
			} else {
				return true;
			}
		};
	}

	/**
	 * makes sure that only options are suggested which are not already part of the uri
	 * 
	 * @param alreadyDefinedOptions	a set of already defined options
	 * @param positionInCamelURI	the position inside the camel uri
	 * @return	the predicate
	 */
	public static Predicate<CompletionItem> removeDuplicatedOptions(Set<OptionParamURIInstance> alreadyDefinedOptions, int positionInCamelURI) {
		return uriOption -> {
			int occured = 0;
			for (OptionParamURIInstance definedOption : alreadyDefinedOptions) {
				if (definedOption.getKey().getKeyName().equalsIgnoreCase(uriOption.getLabel()) && !definedOption.isInRange(positionInCamelURI) ) {
					// found dupe
					occured++;
				}
			}
			return occured < 1;
		};
	}

	/**
	 * ensures that only items with the correct group (either consumer or producer) are
	 * in the list of possible completion items
	 * 
	 * @param isProducer	flag if endpoint is producer endpoint or not
	 * @return	the predicate
	 */
	public static Predicate<EndpointOptionModel> matchesProducerConsumerGroups(boolean isProducer) {
		return endpoint -> {
			String group = endpoint.getGroup();
			if (isProducer) {
				return !"consumer".equals(group);
			} else {
				return !"producer".equals(group);
			}
		};
	}
}
