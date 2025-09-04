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
import java.util.Map.Entry;

import org.apache.camel.util.json.DeserializationException;
import org.apache.camel.util.json.JsonObject;
import org.apache.camel.util.json.Jsoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.model.ApiOptionMethodDescriptorModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiOptionMethodsModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiPropertyMethodOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiPropertyOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.BaseOptionModel;
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
			populateComponentProperties(obj, component);
			populateProperties(obj, component);
			populateApis(obj, component);
			populateApiProperties(obj, component);
		}
		return component;
	}

	private static void populateApiProperties(JsonObject obj, ComponentModel component) {
		Map<String, Map<String, Object>> apiProperties = obj.getMap("apiProperties");
		if (apiProperties != null) {
			for (Map.Entry<String, Map<String, Object>> apiProperty : apiProperties.entrySet()) {
				ApiPropertyOptionModel option = createApiPropertyOptionModel(apiProperty);
				component.addApiPropertyOptionModel(option);
			}
		}
	}

	private static void populateApis(JsonObject obj, ComponentModel component) {
		Map<String, Map<String, Object>> apis = obj.getMap("apis");
		if (apis != null) {
			for (Map.Entry<String, Map<String, Object>> api : apis.entrySet()) {
				ApiOptionModel option = createApiOptionModel(api);
				component.addApiOption(option);
			}
		}
	}

	private static void populateProperties(JsonObject obj, ComponentModel component) {
		Map<String, Map<String, Object>> modelProperties = obj.getMap("properties");
		if (modelProperties != null) {
			for (Map.Entry<String, Map<String, Object>> modelProperty : modelProperties.entrySet()) {
				EndpointOptionModel option = createEndpointOptionModel(modelProperty);
				component.addEndpointOption(option);
			}
		}
	}

	private static void populateComponentProperties(JsonObject obj, ComponentModel component) {
		Map<String, Map<String, Object>> modelComponentProperties = obj.getMap("componentProperties");
		if (modelComponentProperties != null) {
			for (Map.Entry<String, Map<String, Object>> modelComponentProperty : modelComponentProperties.entrySet()) {
				ComponentOptionModel option = createComponentOptionModel(modelComponentProperty);
				component.addComponentOption(option);
			}
		}
	}

	private static ApiPropertyOptionModel createApiPropertyOptionModel(
			Map.Entry<String, Map<String, Object>> apiProperty) {
		ApiPropertyOptionModel option = new ApiPropertyOptionModel();
		option.setName(apiProperty.getKey());
		Map<String, Map<String, Object>> methods = (Map<String, Map<String, Object>>) apiProperty.getValue().getOrDefault("methods", Collections.emptyMap());
		for (Map.Entry<String, Map<String, Object>> method : methods.entrySet()) {
			String methodName = method.getKey();
			ApiPropertyMethodOptionModel methodDescriptor = new ApiPropertyMethodOptionModel();
			Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) method.getValue().getOrDefault("properties", Collections.emptyMap());
			for (Entry<String, Map<String, Object>> propertyEntry : properties.entrySet()) {
				methodDescriptor.add(createEndpointOptionModel(propertyEntry));
			}
			if("creator".equals(methodName)) {
				option.setCreator(methodDescriptor);
			} else if("deleter".equals(methodName)) {
				option.setDeleter(methodDescriptor);
			} else if("fetcher".equals(methodName)) {
				option.setFetcher(methodDescriptor);
			} else if("reader".equals(methodName)) {
				option.setReader(methodDescriptor);
			} else if("updater".equals(methodName)) {
				option.setUpdater(methodDescriptor);
			} else {
				option.addPropertyMethod(methodName, methodDescriptor);
			}
		}
		return option;
	}

	private static ApiOptionModel createApiOptionModel(Map.Entry<String, Map<String, Object>> api) {
		ApiOptionModel option = new ApiOptionModel();
		option.setName(api.getKey());
		Map<String, Object> options = api.getValue();
		option.setConsumerOnly(getSafeBoolean("consumerOnly", options));
		option.setProducerOnly(getSafeBoolean("producerOnly", options));
		option.setAliases((List<String>) options.getOrDefault("aliases", Collections.emptyList()));
		Map<String, Map<String, Object>> methods = (Map<String, Map<String, Object>>) options.getOrDefault("methods", Collections.emptyMap());
		ApiOptionMethodsModel apiOptionsMethodsModel = new ApiOptionMethodsModel();
		for (Map.Entry<String, Map<String, Object>> method : methods.entrySet()) {
			String methodName = method.getKey();
			ApiOptionMethodDescriptorModel methodDescriptor = new ApiOptionMethodDescriptorModel();
			methodDescriptor.setDescription((String)method.getValue().getOrDefault("description", ""));
			methodDescriptor.setSignatures((List<String>)method.getValue().getOrDefault("signatures", Collections.emptyList()));
			if("creator".equals(methodName)) {
				apiOptionsMethodsModel.setCreator(methodDescriptor);
			} else if("deleter".equals(methodName)) {
				apiOptionsMethodsModel.setDeleter(methodDescriptor);
			} else if("fetcher".equals(methodName)) {
				apiOptionsMethodsModel.setFetcher(methodDescriptor);
			} else if("reader".equals(methodName)) {
				apiOptionsMethodsModel.setReader(methodDescriptor);
			} else if("updater".equals(methodName)) {
				apiOptionsMethodsModel.setUpdater(methodDescriptor);
			}
		}
		option.setApiOptionsMethodsModel(apiOptionsMethodsModel);
		return option;
	}

	private static EndpointOptionModel createEndpointOptionModel(Map.Entry<String, Map<String, Object>> modelProperty) {
		EndpointOptionModel option = new EndpointOptionModel();
		fillBaseOptions(modelProperty, option);
		Map<String, Object> options = modelProperty.getValue();
		option.setPrefix((String) options.getOrDefault("prefix", ""));
		option.setMultiValue(getSafeBoolean("multiValue", options));
		return option;
	}

	private static ComponentOptionModel createComponentOptionModel(Map.Entry<String, Map<String, Object>> modelComponentProperty) {
		ComponentOptionModel option = new ComponentOptionModel();
		fillBaseOptions(modelComponentProperty, option);
		return option;
	}

	private static void fillBaseOptions(Map.Entry<String, Map<String, Object>> modelComponentProperty, BaseOptionModel option) {
		Map<String, Object> options = modelComponentProperty.getValue();
		option.setName(modelComponentProperty.getKey());
		option.setKind((String) options.getOrDefault("kind", ""));
		option.setGroup((String) options.getOrDefault("group", ""));
		option.setRequired(getSafeBoolean("required", options));
		option.setType((String) options.getOrDefault("type", ""));
		option.setJavaType((String) options.getOrDefault("javaType", ""));
		option.setDeprecated(getSafeBoolean("deprecated", options));
		option.setSecret(getSafeBoolean("secret", options));
		option.setDescription((String) options.getOrDefault("description", ""));
		option.setEnums((List<String>) options.getOrDefault("enum", Collections.emptyList()));
		option.setDefaultValue(options.getOrDefault("defaultValue", ""));
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
