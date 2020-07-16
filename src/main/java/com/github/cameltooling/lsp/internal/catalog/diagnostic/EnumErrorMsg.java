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
package com.github.cameltooling.lsp.internal.catalog.diagnostic;

import java.util.Arrays;
import java.util.Map;

import org.apache.camel.catalog.ConfigurationPropertiesValidationResult;
import org.apache.camel.catalog.EndpointValidationResult;

public class EnumErrorMsg {

	public String getErrorMessage(EndpointValidationResult result, Map.Entry<String, String> entry) {
		Map<String, String[]> invalidEnumChoices = result.getInvalidEnumChoices();
		Map<String, String> defaultValues = result.getDefaultValues();
		return getErrorMessage(entry, invalidEnumChoices, defaultValues);
	}

	public String getErrorMessage(ConfigurationPropertiesValidationResult result, Map.Entry<String, String> entry) {
		Map<String, String[]> invalidEnumChoices = result.getInvalidEnumChoices();
		Map<String, String> defaultValues = result.getDefaultValues();
		return getErrorMessage(entry, invalidEnumChoices, defaultValues);
	}

	private String getErrorMessage(Map.Entry<String, String> entry, Map<String, String[]> invalidEnumChoices, Map<String, String> defaultValues) {
		String name = entry.getKey();
		String[] choices = invalidEnumChoices.get(name);
		String defaultValue = defaultValues != null ? defaultValues.get(entry.getKey()) : null;
		String str = Arrays.asList(choices).toString();
		String msg = "Invalid enum value: " + entry.getValue() + ". Possible values: " + str;
		if (choices != null && choices.length > 0) {
			str = Arrays.asList(choices).toString();
			msg += ". Did you mean: " + str;
		}
		if (defaultValue != null) {
			msg += ". Default value: " + defaultValue;
		}
		return msg;
	}
}
