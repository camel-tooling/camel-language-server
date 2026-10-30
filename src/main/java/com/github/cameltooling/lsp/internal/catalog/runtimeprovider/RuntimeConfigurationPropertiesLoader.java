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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.VersionManager;
import org.apache.camel.tooling.model.JBangModel;
import org.apache.camel.tooling.model.JBangModel.JBangGroupModel;
import org.apache.camel.tooling.model.MainModel;
import org.apache.camel.tooling.model.MainModel.MainOptionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Loads the configuration properties that a version of a runtime adds to the ones of Camel Main, from the metadata in
 * the runtime artifact: the Spring Boot configuration metadata of camel-spring-boot, the Quarkus configuration model of
 * camel-quarkus-core. The ones of Camel JBang come from the Camel catalog.
 */
public class RuntimeConfigurationPropertiesLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeConfigurationPropertiesLoader.class);

	static final String SPRING_BOOT_METADATA = "META-INF/spring-configuration-metadata.json";
	static final String QUARKUS_CONFIG_MODEL = "META-INF/quarkus-config-doc/quarkus-config-model.json";
	static final String QUARKUS_CONFIG_JAVADOC = "META-INF/quarkus-config-doc/quarkus-config-javadoc.json";

	private static final Pattern ASCIIDOC_SOURCE_BLOCK = Pattern.compile("\\[source(?:,([\\w-]+))?\\]\\n-{4}\\n(.*?)\\n-{4}", Pattern.DOTALL);
	private static final Pattern ASCIIDOC_LINK = Pattern.compile("link:(\\S+?)\\[([^\\]]*)\\]");
	private static final Pattern ASCIIDOC_XREF = Pattern.compile("xref:\\S+?\\[([^\\]]*)\\]");
	private static final Pattern ASCIIDOC_PASSTHROUGH = Pattern.compile("\\+\\+(.+?)\\+\\+");
	private static final Pattern JAVADOC_INLINE_TAG = Pattern.compile("\\{@(?:link|linkplain|code|literal)\\s+([^}]*)\\}");

	private final VersionManager versionManager;

	public RuntimeConfigurationPropertiesLoader(VersionManager versionManager) {
		this.versionManager = versionManager;
	}

	/**
	 * @param camelMain the Camel Main model of the catalog, whose properties are left out
	 * @return the configuration properties, {@link RuntimeConfigurationProperties#NONE} when the runtime adds none or
	 * when its metadata cannot be loaded
	 */
	public RuntimeConfigurationProperties load(CamelRuntimeProvider runtimeProvider, String version, MainModel camelMain) {
		String artifactId = runtimeProvider.getConfigurationArtifactId();
		if (artifactId == null || !versionManager.loadRuntimeProviderVersion(runtimeProvider.getCatalogGroupId(), artifactId, version)) {
			return RuntimeConfigurationProperties.NONE;
		}
		String runtime = runtimeProvider.getTitle() + " " + version;
		String jarName = artifactId + "-" + version + ".jar";
		try {
			List<RuntimeConfigurationProperty> properties;
			switch (runtimeProvider) {
			case SPRINGBOOT:
				properties = readSpringBootMetadata(runtime, jarName, camelMain);
				break;
			case QUARKUS:
				properties = readQuarkusConfigModel(runtime, jarName);
				break;
			default:
				properties = Collections.emptyList();
			}
			return new RuntimeConfigurationProperties(properties);
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Cannot read the configuration metadata of {}", jarName, e);
			return RuntimeConfigurationProperties.NONE;
		}
	}

	/**
	 * @return the configuration properties of Camel JBang, the Camel CLI, documented in the catalog,
	 * {@link RuntimeConfigurationProperties#NONE} when the catalog has none
	 */
	public static RuntimeConfigurationProperties loadCamelJBang(CamelCatalog catalog) {
		JBangModel model;
		try {
			model = catalog.jbangModel();
		} catch (RuntimeException e) {
			LOGGER.warn("Cannot read the Camel JBang configuration metadata of the catalog {}", catalog.getCatalogVersion(), e);
			return RuntimeConfigurationProperties.NONE;
		}
		if (model == null) {
			return RuntimeConfigurationProperties.NONE;
		}
		String runtime = "Camel JBang " + catalog.getCatalogVersion();
		List<RuntimeConfigurationProperty> properties = model.getOptions().stream()
				.map(option -> new RuntimeConfigurationProperty(runtime, option.getName(), option.getDescription(), option.getType(),
						option.getDefaultValue() != null ? option.getDefaultValue().toString() : null, option.getEnums(),
						option.isDeprecated() ? Objects.toString(option.getDeprecationNote(), "") : null))
				.collect(Collectors.toList());
		Map<String, String> groupDescriptions = model.getGroups().stream()
				.filter(group -> group.getDescription() != null)
				.collect(Collectors.toMap(JBangGroupModel::getName, JBangGroupModel::getDescription, (first, second) -> first));
		return new RuntimeConfigurationProperties(properties, groupDescriptions);
	}

	private List<RuntimeConfigurationProperty> readSpringBootMetadata(String runtime, String jarName, MainModel camelMain) throws IOException {
		try (Reader metadata = openResource(jarName, SPRING_BOOT_METADATA)) {
			if (metadata == null) {
				return Collections.emptyList();
			}
			return parseSpringBootMetadata(runtime, metadata, camelMain.getOptions().stream().map(MainOptionModel::getName).collect(Collectors.toList()));
		}
	}

	private List<RuntimeConfigurationProperty> readQuarkusConfigModel(String runtime, String jarName) throws IOException {
		try (Reader model = openResource(jarName, QUARKUS_CONFIG_MODEL); Reader javadoc = openResource(jarName, QUARKUS_CONFIG_JAVADOC)) {
			if (model == null) {
				// before Camel Quarkus 3.27, the configuration is documented in a legacy format without the property names
				return Collections.emptyList();
			}
			return parseQuarkusConfigModel(runtime, model, javadoc);
		}
	}

	/**
	 * @return the resource in the JAR of this name among the JARs loaded by the version manager, null when it has none
	 */
	private Reader openResource(String jarName, String resource) throws IOException {
		Enumeration<URL> urls = versionManager.getClassLoader().getResources(resource);
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			if (url.toString().contains("/" + jarName + "!/")) {
				return new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
			}
		}
		return null;
	}

	/**
	 * @param runtime the runtime and its version
	 * @param camelMainOptionNames the names of the Camel Main options, whose properties are left out
	 */
	static List<RuntimeConfigurationProperty> parseSpringBootMetadata(String runtime, Reader metadata, Collection<String> camelMainOptionNames) {
		Set<String> camelMain = camelMainOptionNames.stream().map(RuntimeConfigurationProperties::normalize).collect(Collectors.toSet());
		List<RuntimeConfigurationProperty> properties = new ArrayList<>();
		JsonArray metadataProperties = JsonParser.parseReader(metadata).getAsJsonObject().getAsJsonArray("properties");
		if (metadataProperties == null) {
			return properties;
		}
		for (JsonElement element : metadataProperties) {
			JsonObject property = element.getAsJsonObject();
			String name = getString(property, "name");
			if (name != null && !camelMain.contains(RuntimeConfigurationProperties.normalize(name))) {
				JsonObject deprecation = getObject(property, "deprecation");
				boolean deprecated = getBoolean(property, "deprecated") || deprecation != null;
				properties.add(new RuntimeConfigurationProperty(runtime, name, getString(property, "description"), getString(property, "type"),
						getValue(property, "defaultValue"), Collections.emptyList(), deprecated ? getDeprecationNote(deprecation) : null));
			}
		}
		return properties;
	}

	/**
	 * @param runtime the runtime and its version
	 * @param javadoc the descriptions of the properties, null when there are none
	 */
	static List<RuntimeConfigurationProperty> parseQuarkusConfigModel(String runtime, Reader model, Reader javadoc) {
		JsonObject descriptions = javadoc != null ? getObject(JsonParser.parseReader(javadoc).getAsJsonObject(), "elements") : null;
		List<RuntimeConfigurationProperty> properties = new ArrayList<>();
		JsonArray configRoots = JsonParser.parseReader(model).getAsJsonObject().getAsJsonArray("configRoots");
		if (configRoots != null) {
			for (JsonElement configRoot : configRoots) {
				collectQuarkusProperties(runtime, configRoot.getAsJsonObject().getAsJsonArray("items"), descriptions, properties);
			}
		}
		return properties;
	}

	private static void collectQuarkusProperties(String runtime, JsonArray items, JsonObject descriptions, List<RuntimeConfigurationProperty> properties) {
		if (items == null) {
			return;
		}
		for (JsonElement element : items) {
			JsonObject item = element.getAsJsonObject();
			if (item.has("items")) {
				// a section, grouping properties
				collectQuarkusProperties(runtime, item.getAsJsonArray("items"), descriptions, properties);
			} else {
				JsonObject path = getObject(item, "path");
				String name = path != null ? getString(path, "property") : null;
				if (name != null) {
					JsonObject deprecation = getObject(item, "deprecation");
					boolean deprecated = deprecation != null && deprecation.size() > 0;
					String description = getQuarkusDescription(descriptions, getString(item, "sourceType") + "." + getString(item, "sourceElementName"));
					properties.add(new RuntimeConfigurationProperty(runtime, name, description, getQuarkusType(item), getValue(item, "defaultValue"),
							getQuarkusEnumValues(item), deprecated ? getDeprecationNote(deprecation) : null));
				}
			}
		}
	}

	private static String getQuarkusDescription(JsonObject descriptions, String sourceElement) {
		JsonObject javadoc = descriptions != null ? getObject(descriptions, sourceElement) : null;
		String description = javadoc != null ? getString(javadoc, "description") : null;
		return description != null ? toMarkdown(description) : null;
	}

	private static String getQuarkusType(JsonObject item) {
		String type = getString(item, "typeDescription");
		if (type == null) {
			type = getString(item, "type");
		}
		return type != null && getBoolean(item, "list") ? "list of " + type : type;
	}

	private static List<String> getQuarkusEnumValues(JsonObject item) {
		JsonObject enumAcceptedValues = getObject(item, "enumAcceptedValues");
		JsonObject values = enumAcceptedValues != null ? getObject(enumAcceptedValues, "values") : null;
		if (values == null) {
			return Collections.emptyList();
		}
		List<String> enumValues = new ArrayList<>();
		for (Map.Entry<String, JsonElement> value : values.entrySet()) {
			String configValue = value.getValue().isJsonObject() ? getString(value.getValue().getAsJsonObject(), "configValue") : null;
			enumValues.add(configValue != null ? configValue : value.getKey());
		}
		return enumValues;
	}

	/**
	 * @return what to know about the deprecation, empty when the metadata tells nothing
	 */
	private static String getDeprecationNote(JsonObject deprecation) {
		if (deprecation == null) {
			return "";
		}
		List<String> notes = new ArrayList<>();
		String since = getString(deprecation, "since");
		if (since != null) {
			notes.add("since " + since);
		}
		String replacement = getString(deprecation, "replacement");
		if (replacement != null) {
			notes.add("use " + replacement + " instead");
		}
		String reason = getString(deprecation, "reason");
		if (reason != null) {
			notes.add(reason);
		}
		return String.join(", ", notes);
	}

	/**
	 * Converts the markup used in the descriptions of the Quarkus configuration: the AsciiDoc source blocks, links and
	 * passthroughs, and the Javadoc inline tags left in some descriptions.
	 */
	static String toMarkdown(String quarkusDescription) {
		String markdown = ASCIIDOC_SOURCE_BLOCK.matcher(quarkusDescription).replaceAll(
				match -> Matcher.quoteReplacement("```" + Objects.toString(match.group(1), "") + "\n" + match.group(2) + "\n```"));
		markdown = ASCIIDOC_LINK.matcher(markdown).replaceAll(
				match -> Matcher.quoteReplacement(match.group(2).isEmpty() ? match.group(1) : "[" + match.group(2) + "](" + match.group(1) + ")"));
		markdown = ASCIIDOC_XREF.matcher(markdown).replaceAll(match -> Matcher.quoteReplacement(match.group(1)));
		markdown = ASCIIDOC_PASSTHROUGH.matcher(markdown).replaceAll(match -> Matcher.quoteReplacement(match.group(1)));
		return JAVADOC_INLINE_TAG.matcher(markdown).replaceAll(match -> Matcher.quoteReplacement("`" + match.group(1).trim() + "`"));
	}

	private static String getString(JsonObject object, String key) {
		JsonElement value = object.get(key);
		return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
	}

	private static boolean getBoolean(JsonObject object, String key) {
		JsonElement value = object.get(key);
		return value != null && value.isJsonPrimitive() && value.getAsBoolean();
	}

	private static JsonObject getObject(JsonObject object, String key) {
		JsonElement value = object.get(key);
		return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
	}

	private static String getValue(JsonObject object, String key) {
		JsonElement value = object.get(key);
		if (value == null || value.isJsonNull()) {
			return null;
		} else if (value.isJsonPrimitive()) {
			return value.getAsString();
		} else if (value.isJsonArray()) {
			List<String> values = new ArrayList<>();
			for (JsonElement item : value.getAsJsonArray()) {
				values.add(item.isJsonPrimitive() ? item.getAsString() : item.toString());
			}
			return String.join(",", values);
		}
		return value.toString();
	}
}
