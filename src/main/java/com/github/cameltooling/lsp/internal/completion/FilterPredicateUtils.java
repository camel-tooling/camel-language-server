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
package com.github.cameltooling.lsp.internal.completion;

import java.util.function.Predicate;

import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.model.EndpointOptionModel;

public class FilterPredicateUtils {
	
	private FilterPredicateUtils() {
		// util class
	}
	
	public static Predicate<CompletionItem> matchesCompletionFilter(String filterString) {
        return item -> filterString != null && filterString.trim().length() > 0 ? item.getLabel().startsWith(filterString) : true;
    }
	
	public static Predicate<EndpointOptionModel> matchesEndpointOptionFilter(String filterString) {
        return item -> filterString != null && filterString.trim().length()>0 ? item.getName().startsWith(filterString) : true;
    }
}
