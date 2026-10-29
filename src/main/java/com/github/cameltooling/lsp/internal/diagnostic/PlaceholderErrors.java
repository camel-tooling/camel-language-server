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
package com.github.cameltooling.lsp.internal.diagnostic;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.tooling.model.ApiModel;
import org.apache.camel.tooling.model.ComponentModel;
import org.apache.camel.util.StringHelper;

/**
 * The errors that the Camel catalog reports when validating an endpoint because of property placeholders, which are
 * resolved at runtime only. The catalog skips the placeholders except in the API name and the method name of an API
 * component, for instance {@code fhir://{{apiName}}/{{methodName}}}: it reports them as invalid enum values, and the
 * parameters of the method it cannot resolve as unknown options.
 */
final class PlaceholderErrors {

	private static final PlaceholderErrors NONE = new PlaceholderErrors(Collections.emptySet(), Collections.emptySet());
	private static final String PLACEHOLDER_PREFIX = "{{";
	private static final String API_NAME = "apiName";
	private static final String METHOD_NAME = "methodName";

	private final Set<String> invalidEnums;
	private final Set<String> unknownOptions;

	private PlaceholderErrors(Set<String> invalidEnums, Set<String> unknownOptions) {
		this.invalidEnums = invalidEnums;
		this.unknownOptions = unknownOptions;
	}

	/**
	 * @param catalog the catalog that validated the endpoint, null when it is not known
	 */
	static PlaceholderErrors of(CamelCatalog catalog, String endpointUri, EndpointValidationResult result) {
		Map<String, String> invalidEnum = result.getInvalidEnum();
		if (catalog == null || invalidEnum == null) {
			return NONE;
		}
		Set<String> placeholderEnums = invalidEnum.entrySet().stream()
				.filter(entry -> entry.getValue() != null && entry.getValue().contains(PLACEHOLDER_PREFIX))
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());
		if (placeholderEnums.isEmpty()) {
			return NONE;
		}
		Set<String> placeholderUnknownOptions = Collections.emptySet();
		if (result.getUnknown() != null && (placeholderEnums.contains(API_NAME) || placeholderEnums.contains(METHOD_NAME))) {
			Set<String> methodParameters = getMethodParameters(catalog, endpointUri, placeholderEnums.contains(API_NAME));
			placeholderUnknownOptions = result.getUnknown().stream()
					.filter(option -> methodParameters.contains(StringHelper.dashToCamelCase(option)))
					.collect(Collectors.toSet());
		}
		return new PlaceholderErrors(placeholderEnums, placeholderUnknownOptions);
	}

	/**
	 * @return the parameters of the methods of the API of the endpoint, or of all the APIs of its component when the
	 * API name is a placeholder too, so that the options that no method has are still reported as unknown
	 */
	private static Set<String> getMethodParameters(CamelCatalog catalog, String endpointUri, boolean apiNameIsPlaceholder) {
		String scheme = StringHelper.before(endpointUri, ":");
		ComponentModel component = scheme != null ? catalog.componentModel(scheme) : null;
		if (component == null || !component.isApi()) {
			return Collections.emptySet();
		}
		Collection<ApiModel> apis = component.getApiOptions();
		String apiName = apiNameIsPlaceholder ? null : getApiName(catalog, endpointUri);
		if (apiName != null && apis.stream().anyMatch(api -> apiName.equals(api.getName()))) {
			apis = apis.stream().filter(api -> apiName.equals(api.getName())).collect(Collectors.toList());
		}
		return apis.stream()
				.flatMap(api -> api.getMethods().stream())
				.flatMap(method -> method.getOptions().stream())
				.map(ComponentModel.ApiOptionModel::getName)
				.collect(Collectors.toSet());
	}

	private static String getApiName(CamelCatalog catalog, String endpointUri) {
		try {
			return catalog.endpointProperties(endpointUri).get(API_NAME);
		} catch (URISyntaxException e) {
			return null;
		}
	}

	/**
	 * @return whether the invalid value of this option is a placeholder
	 */
	boolean isInvalidEnum(String option) {
		return invalidEnums.contains(option);
	}

	/**
	 * @return whether this option is unknown because the method of the API is a placeholder
	 */
	boolean isUnknownOption(String option) {
		return unknownOptions.contains(option);
	}

	int getNumberOfErrors() {
		return invalidEnums.size() + unknownOptions.size();
	}
}
