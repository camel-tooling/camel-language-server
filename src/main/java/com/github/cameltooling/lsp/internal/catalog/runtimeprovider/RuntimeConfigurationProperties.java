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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;

/**
 * The configuration properties that Camel JBang and the runtime of a Camel catalog add to the ones of Camel Main,
 * for instance the {@code camel.jbang.*} properties of Camel JBang and the {@code quarkus.camel.*} properties of
 * Camel Quarkus. Keys are matched in kebab case and in camel case alike.
 */
public class RuntimeConfigurationProperties {

	public static final RuntimeConfigurationProperties NONE = new RuntimeConfigurationProperties(Collections.emptyList());

	private static final Map<CamelCatalog, RuntimeConfigurationProperties> PROPERTIES_BY_CATALOG = Collections.synchronizedMap(new WeakHashMap<>());

	private final Map<String, RuntimeConfigurationProperty> properties = new LinkedHashMap<>();
	private final Map<String, String> groupDescriptions = new HashMap<>();

	public RuntimeConfigurationProperties(Collection<RuntimeConfigurationProperty> properties) {
		this(properties, Collections.emptyMap());
	}

	/**
	 * @param groupDescriptions the descriptions of the groups of properties, by group name, for instance {@code camel.jbang}
	 */
	public RuntimeConfigurationProperties(Collection<RuntimeConfigurationProperty> properties, Map<String, String> groupDescriptions) {
		for (RuntimeConfigurationProperty property : properties) {
			this.properties.putIfAbsent(normalize(property.getName()), property);
		}
		groupDescriptions.forEach((group, description) -> this.groupDescriptions.putIfAbsent(normalize(group), description));
	}

	/**
	 * @return the configuration properties of the runtime of this catalog, {@link #NONE} when none were registered
	 */
	public static RuntimeConfigurationProperties of(CamelCatalog catalog) {
		return PROPERTIES_BY_CATALOG.getOrDefault(catalog, NONE);
	}

	public static void register(CamelCatalog catalog, RuntimeConfigurationProperties properties) {
		PROPERTIES_BY_CATALOG.put(catalog, properties);
	}

	/**
	 * @return the properties of this and the other, the ones of this first
	 */
	public RuntimeConfigurationProperties with(RuntimeConfigurationProperties other) {
		List<RuntimeConfigurationProperty> allProperties = new ArrayList<>(properties.values());
		allProperties.addAll(other.properties.values());
		Map<String, String> allGroupDescriptions = new HashMap<>(other.groupDescriptions);
		allGroupDescriptions.putAll(groupDescriptions);
		return new RuntimeConfigurationProperties(allProperties, allGroupDescriptions);
	}

	public Collection<RuntimeConfigurationProperty> getProperties() {
		return properties.values();
	}

	public boolean isEmpty() {
		return properties.isEmpty();
	}

	public Optional<RuntimeConfigurationProperty> find(String key) {
		return Optional.ofNullable(properties.get(normalize(key)));
	}

	public List<RuntimeConfigurationProperty> findStartingWith(String keyPrefix) {
		String normalizedPrefix = normalize(keyPrefix);
		return properties.entrySet().stream()
				.filter(entry -> entry.getKey().startsWith(normalizedPrefix))
				.map(Map.Entry::getValue)
				.collect(Collectors.toList());
	}

	/**
	 * @return the description of the group of properties with this name, null when it has none
	 */
	public String getGroupDescription(String group) {
		return groupDescriptions.get(normalize(group));
	}

	static String normalize(String key) {
		return key.replace("-", "").toLowerCase(Locale.ROOT);
	}
}
