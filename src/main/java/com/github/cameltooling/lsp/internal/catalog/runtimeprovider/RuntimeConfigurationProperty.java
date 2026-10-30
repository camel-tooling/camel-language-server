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
package com.github.cameltooling.lsp.internal.catalog.runtimeprovider;

import java.util.Collections;
import java.util.List;

/**
 * A configuration property that a runtime adds to the ones of Camel Main, for instance {@code quarkus.camel.bootstrap.enabled}
 * on Camel Quarkus, or {@code camel.jbang.runtime} on Camel JBang.
 */
public class RuntimeConfigurationProperty {

	private final String runtime;
	private final String name;
	private final String description;
	private final String type;
	private final String defaultValue;
	private final List<String> enumValues;
	private final String deprecationNote;

	/**
	 * @param runtime the runtime adding this property, and its version
	 * @param name the name, as documented by the runtime
	 * @param description the description, in Markdown, or null
	 * @param type the type, as documented by the runtime, or null
	 * @param defaultValue the default value, or null
	 * @param enumValues the possible values, empty or null when any value of the type is possible
	 * @param deprecationNote null when the property is not deprecated, otherwise what to know about the deprecation,
	 * for instance the property to use instead, or an empty string
	 */
	public RuntimeConfigurationProperty(String runtime, String name, String description, String type, String defaultValue, List<String> enumValues,
			String deprecationNote) {
		this.runtime = runtime;
		this.name = name;
		this.description = description;
		this.type = type;
		this.defaultValue = defaultValue;
		this.enumValues = enumValues != null ? enumValues : Collections.emptyList();
		this.deprecationNote = deprecationNote;
	}

	public String getRuntime() {
		return runtime;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getType() {
		return type;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public List<String> getEnumValues() {
		return enumValues;
	}

	public boolean isDeprecated() {
		return deprecationNote != null;
	}

	public String getDeprecationNote() {
		return deprecationNote;
	}

	public boolean isBoolean() {
		return "boolean".equals(type) || Boolean.class.getName().equals(type);
	}
}
