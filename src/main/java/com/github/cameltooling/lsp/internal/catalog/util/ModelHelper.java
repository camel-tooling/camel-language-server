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
package com.github.cameltooling.lsp.internal.catalog.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.camel.util.json.DeserializationException;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.model.ComponentOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.EndpointOptionModel;

public final class ModelHelper {

	private ModelHelper() {
		// utility class
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ModelHelper.class);

	public static ComponentModel generateComponentModel(String json, boolean includeOptions) {
		JsonObject obj;
		if (json != null) {
			try {
				obj = (JsonObject) Jsoner.deserialize(json);
				return generateComponentModel(includeOptions, obj);
			} catch (DeserializationException e) {
				LOGGER.error("Cannot deserialize provided json.", e);
				return new ComponentModel();
			}
		} else {
			return new ComponentModel();
		}
	}

	private static ComponentModel generateComponentModel(boolean includeOptions, JsonObject obj) {
		ComponentModel component = new ComponentModel();
		Map<String, Object> modelComponent = obj.getMap("component");

		component.setScheme((String) modelComponent.getOrDefault("scheme", ""));
		component.setSyntax((String) modelComponent.getOrDefault("syntax", ""));
		component.setAlternativeSyntax((String) modelComponent.getOrDefault("alternativeSyntax", ""));
		component.setAlternativeSchemes((String) modelComponent.getOrDefault("alternativeSchemes", ""));
		component.setTitle((String) modelComponent.getOrDefault("title", ""));
		component.setDescription((String) modelComponent.getOrDefault("description", ""));
		component.setLabel((String) modelComponent.getOrDefault("label", ""));
		component.setDeprecated(getSafeBoolean("deprecated", modelComponent));
		component.setConsumerOnly(getSafeBoolean("consumerOnly", modelComponent));
		component.setProducerOnly(getSafeBoolean("producerOnly", modelComponent));
		component.setJavaType((String) modelComponent.getOrDefault("javaType", ""));
		component.setGroupId((String) modelComponent.getOrDefault("groupId", ""));
		component.setArtifactId((String) modelComponent.getOrDefault("artifactId", ""));
		component.setVersion((String) modelComponent.getOrDefault("version", ""));

		if (includeOptions) {
			Map<String, Map<String, Object>> modelComponentProperties = obj.getMap("componentProperties");
			if (modelComponentProperties != null) {
				for (Map.Entry<String, Map<String, Object>> modelComponentProperty : modelComponentProperties
						.entrySet()) {
					ComponentOptionModel option = new ComponentOptionModel();
					Map<String, Object> options = modelComponentProperty.getValue();
					option.setName(modelComponentProperty.getKey());
					option.setKind((String) options.getOrDefault("kind", ""));
					option.setGroup((String) options.getOrDefault("group", ""));
					option.setRequired(getSafeBoolean("required", options));
					option.setType((String) options.getOrDefault("type", ""));
					option.setJavaType((String) options.getOrDefault("javaType", ""));
					option.setEnums((List<String>) options.getOrDefault("enum", Collections.emptyList()));
					option.setDeprecated(getSafeBoolean("deprecated", options));
					option.setSecret(getSafeBoolean("secret", options));
					option.setDefaultValue(options.getOrDefault("defaultValue", ""));
					option.setDescription((String) options.getOrDefault("description", ""));
					component.addComponentOption(option);
				}
			}

			Map<String, Map<String, Object>> modelProperties = obj.getMap("properties");
			if (modelProperties != null) {
				for (Map.Entry<String, Map<String, Object>> modelProperty : modelProperties.entrySet()) {
					EndpointOptionModel option = new EndpointOptionModel();
					Map<String, Object> options = modelProperty.getValue();
					option.setName(modelProperty.getKey());
					option.setKind((String) options.getOrDefault("kind", ""));
					option.setGroup((String) options.getOrDefault("group", ""));
					option.setRequired(getSafeBoolean("required", options));
					option.setType((String) options.getOrDefault("type", ""));
					option.setJavaType((String) options.getOrDefault("javaType", ""));
					option.setEnums((List<String>) options.getOrDefault("enum", Collections.emptyList()));
					option.setPrefix((String) options.getOrDefault("prefix", ""));
					option.setMultiValue(getSafeBoolean("multiValue", options));
					option.setDeprecated(getSafeBoolean("deprecated", options));
					option.setSecret(getSafeBoolean("secret", options));
					option.setDefaultValue(options.getOrDefault("defaultValue", ""));
					option.setDescription((String) options.getOrDefault("description", ""));
					component.addEndpointOption(option);
				}
			}
		}
		return component;
	}

	/**
	 * some old catalog are using strings for the boolean values for some of the
	 * boolean type for instance see grape in 2.23.4 Catalog
	 */
	private static boolean getSafeBoolean(String key, Map<String, Object> options) {
		Object value = options.get(key);
		if (value instanceof Boolean) {
			return ((Boolean) value).booleanValue();
		} else if ("true".equals(value)) {
			return true;
		}
		return false;
	}
}
