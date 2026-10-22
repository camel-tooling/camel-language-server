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
package com.github.cameltooling.lsp.internal.catalog.util;

import java.util.Set;

/**
 * The keys that the Language Server needs to find its way in a YAML document, the {@link YamlDslModel} of the Camel
 * catalog tells the rest:
 * <ul>
 * <li>keys of the Camel YAML DSL, which {@code YamlDslKeysTest} checks against the model of the bundled catalog</li>
 * <li>keys of the Kubernetes resources embedding the DSL ({@code camel.apache.org/v1}), which no Camel model describes</li>
 * </ul>
 */
public final class YamlDslKeys {

	public static final String FROM = "from";
	public static final String STEPS = "steps";
	public static final String URI = "uri";
	public static final String PARAMETERS = "parameters";
	public static final String EXPRESSION = "expression";
	public static final String NAME = "name";
	/** The Kamelet EIP, whose parameters are the properties of the Kamelet with the given name. */
	public static final String KAMELET = "kamelet";
	/** The elements consuming from their endpoint, the other ones send to it. */
	public static final Set<String> CONSUMER_ENDPOINTS = Set.of(FROM, "poll");

	public static final String KIND = "kind";
	public static final String SPEC = "spec";
	public static final String TEMPLATE = "template";
	public static final String FLOW = "flow";
	public static final String FLOWS = "flows";
	public static final String SOURCE = "source";
	public static final String SINK = "sink";
	public static final String PROPERTIES = "properties";
	public static final String KIND_KAMELET = "Kamelet";
	public static final String KIND_PIPE = "Pipe";
	public static final String KIND_KAMELET_BINDING = "KameletBinding";
	public static final String KIND_INTEGRATION = "Integration";

	private YamlDslKeys() {
		// constants
	}
}
