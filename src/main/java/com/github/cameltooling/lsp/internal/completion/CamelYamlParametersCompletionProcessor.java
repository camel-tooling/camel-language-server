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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.v1.kameletspec.Definition;
import org.apache.camel.v1.kameletspec.definition.Properties;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.model.EndpointOptionModel;
import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.catalog.util.YamlDslKeys;

/**
 * Completion and hover for the endpoint options written in the {@code parameters} of a Camel YAML DSL endpoint.
 * The component is given by the {@code uri} declared next to {@code parameters}.
 */
public class CamelYamlParametersCompletionProcessor {

	private static final String PATH_KIND = "path";
	private static final Pattern SCHEME = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*");
	private static final Pattern YAML_SPECIAL_CHARACTERS = Pattern.compile("[:#{}\\[\\],&*!|>'\"%@`]|^\\s|\\s$");

	private final YamlDslLines lines;
	private final CompletableFuture<CamelCatalog> camelCatalog;
	private final KameletsCatalogManager kameletsCatalogManager;
	private final boolean markdown;

	public CamelYamlParametersCompletionProcessor(YamlDslLines lines, CompletableFuture<CamelCatalog> camelCatalog, KameletsCatalogManager kameletsCatalogManager, boolean markdown) {
		this.lines = lines;
		this.camelCatalog = camelCatalog;
		this.kameletsCatalogManager = kameletsCatalogManager;
		this.markdown = markdown;
	}

	public boolean isInParametersBlock(Position position) {
		int parent = lines.parent(position);
		return parent >= 0 && YamlDslKeys.PARAMETERS.equals(lines.key(parent)) && lines.isBlockKey(parent);
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		int parametersLine = lines.parent(position);
		Endpoint endpoint = findEndpoint(parametersLine);
		if (endpoint == null) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		String typed = lines.contentBeforeCursor(position);
		int separator = YamlDslLines.separatorIndex(typed);
		Set<String> definedKeys = lines.children(parametersLine).stream()
				.filter(line -> line != position.getLine())
				.map(lines::key)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		return camelCatalog.thenApply(catalog -> {
			ComponentModel componentModel = getComponentModel(catalog, endpoint);
			if (separator > 0) {
				return getValueCompletions(componentModel, typed.substring(0, separator).trim());
			}
			return Stream.concat(getOptionItems(componentModel, endpoint), getKameletPropertyItems(componentModel, endpoint))
					.filter(item -> !definedKeys.contains(item.getLabel()))
					.filter(item -> item.getLabel().startsWith(typed))
					.collect(Collectors.toList());
		});
	}

	public CompletableFuture<Hover> getHover(Position position) {
		int lineNumber = position.getLine();
		String key = lines.key(lineNumber);
		int keyStart = lines.contentColumn(lineNumber);
		int keyEnd = lines.keyEndColumn(lineNumber);
		Endpoint endpoint = findEndpoint(lines.parent(position));
		if (key == null || endpoint == null || position.getCharacter() < keyStart || position.getCharacter() >= keyEnd) {
			return CompletableFuture.completedFuture(null);
		}
		Range range = new Range(new Position(lineNumber, keyStart), new Position(lineNumber, keyEnd));
		return camelCatalog.thenApply(catalog -> {
			Optional<EndpointOptionModel> option = findOption(getComponentModel(catalog, endpoint), key);
			if (option.isPresent()) {
				MarkupContent documentation = markdown
						? CamelOptionNamesCompletionsFuture.getMarkupDocumentation(option.get())
						: new MarkupContent(MarkupKind.PLAINTEXT, CamelOptionNamesCompletionsFuture.getDocumentation(option.get()));
				return new Hover(documentation, range);
			}
			Properties kameletProperty = getKameletProperties(getComponentModel(catalog, endpoint), endpoint).get(key);
			if (kameletProperty != null && kameletProperty.getDescription() != null) {
				return new Hover(new MarkupContent(MarkupKind.PLAINTEXT, kameletProperty.getDescription()), range);
			}
			return null;
		});
	}

	/**
	 * @return the endpoint configured by the given "parameters:" line, or null if it cannot be determined
	 */
	private Endpoint findEndpoint(int parametersLine) {
		if (parametersLine < 0 || !YamlDslKeys.PARAMETERS.equals(lines.key(parametersLine))) {
			return null;
		}
		int owner = lines.parent(parametersLine);
		if (owner < 0) {
			return null;
		}
		List<Integer> mappingLines = new ArrayList<>(lines.children(owner));
		if (lines.isListItem(owner)) {
			// the first key of a sequence entry is written on the line of the entry
			mappingLines.add(owner);
		}
		String uri = null;
		String name = null;
		for (int line : mappingLines) {
			String key = lines.key(line);
			if (YamlDslKeys.URI.equals(key)) {
				uri = YamlDslLines.unquote(lines.value(line));
			} else if (YamlDslKeys.NAME.equals(key)) {
				name = YamlDslLines.unquote(lines.value(line));
			}
		}
		String ownerKey = lines.key(owner);
		if (YamlDslKeys.KAMELET.equals(ownerKey)) {
			// the Kamelet EIP: the parameters are the properties of the Kamelet
			return name != null ? new Endpoint(null, false, name, true, parametersLine) : null;
		}
		if (uri == null) {
			return null;
		}
		int pathStart = uri.indexOf(':');
		String scheme = pathStart >= 0 ? uri.substring(0, pathStart) : uri;
		if (!SCHEME.matcher(scheme).matches()) {
			// placeholder or expression
			return null;
		}
		String kameletTemplateId = YamlDslKeys.KAMELET.equals(scheme) && pathStart >= 0 ? uri.substring(pathStart + 1).split("[?/]")[0] : null;
		return new Endpoint(scheme, pathStart < 0, kameletTemplateId, !YamlDslKeys.CONSUMER_ENDPOINTS.contains(ownerKey), parametersLine);
	}

	private String findParameterValue(int parametersLine, String parameter) {
		return lines.children(parametersLine).stream()
				.filter(line -> parameter.equals(lines.key(line)))
				.map(line -> YamlDslLines.unquote(lines.value(line)))
				.findFirst()
				.orElse(null);
	}

	private ComponentModel getComponentModel(CamelCatalog catalog, Endpoint endpoint) {
		String json = endpoint.scheme != null ? catalog.componentJSonSchema(endpoint.scheme) : null;
		return json != null ? ModelHelper.generateComponentModel(json, true) : null;
	}

	private Stream<CompletionItem> getOptionItems(ComponentModel componentModel, Endpoint endpoint) {
		if (componentModel == null) {
			return Stream.empty();
		}
		return componentModel.getEndpointOptions().stream()
				// with a uri containing only the scheme, the path parameters are written in the parameters too
				.filter(option -> "parameter".equals(option.getKind()) || endpoint.schemeOnly && PATH_KIND.equals(option.getKind()))
				.filter(FilterPredicateUtils.matchesProducerConsumerGroups(endpoint.producer))
				.map(this::createOptionItem);
	}

	private Stream<CompletionItem> getKameletPropertyItems(ComponentModel componentModel, Endpoint endpoint) {
		return getKameletProperties(componentModel, endpoint).entrySet().stream().map(property -> {
			CompletionItem item = new CompletionItem(property.getKey());
			item.setKind(CompletionItemKind.Property);
			Properties schema = property.getValue();
			Object defaultValue = schema.get_default() != null ? schema.get_default().getValue() : null;
			item.setInsertText(property.getKey() + ": " + toYamlValue(defaultValue));
			item.setDetail(schema.getType());
			item.setDocumentation(schema.getDescription());
			return item;
		});
	}

	private Map<String, Properties> getKameletProperties(ComponentModel componentModel, Endpoint endpoint) {
		String kameletTemplateId = getKameletTemplateId(componentModel, endpoint);
		if (kameletTemplateId != null) {
			Definition definition = kameletsCatalogManager.getCatalog().getKameletDefinition(kameletTemplateId);
			if (definition != null && definition.getProperties() != null) {
				return definition.getProperties();
			}
		}
		return Collections.emptyMap();
	}

	/**
	 * @return the name of the Kamelet: the name of the Kamelet EIP, the path of a kamelet URI, or the first path parameter
	 * of the kamelet component written in the parameters of a URI without path
	 */
	private String getKameletTemplateId(ComponentModel componentModel, Endpoint endpoint) {
		if (endpoint.kameletTemplateId != null || !endpoint.schemeOnly || componentModel == null || !YamlDslKeys.KAMELET.equals(endpoint.scheme)) {
			return endpoint.kameletTemplateId;
		}
		return componentModel.getEndpointOptions().stream()
				.filter(option -> PATH_KIND.equals(option.getKind()))
				.findFirst()
				.map(option -> findParameterValue(endpoint.parametersLine, option.getName()))
				.orElse(null);
	}

	private List<CompletionItem> getValueCompletions(ComponentModel componentModel, String key) {
		Optional<EndpointOptionModel> option = findOption(componentModel, key);
		if (option.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> values;
		if (option.get().getEnums() != null && !option.get().getEnums().isEmpty()) {
			values = option.get().getEnums();
		} else if ("boolean".equals(option.get().getType())) {
			values = List.of("true", "false");
		} else {
			return Collections.emptyList();
		}
		Object defaultValue = option.get().getDefaultValue();
		return CamelYamlDslKeywordProcessor.createValueItems(values, defaultValue != null ? defaultValue.toString() : null);
	}

	private Optional<EndpointOptionModel> findOption(ComponentModel componentModel, String name) {
		if (componentModel == null) {
			return Optional.empty();
		}
		return componentModel.getEndpointOptions().stream().filter(option -> name.equals(option.getName())).findFirst();
	}

	private CompletionItem createOptionItem(EndpointOptionModel option) {
		CompletionItem item = new CompletionItem(option.getName());
		item.setKind(CompletionItemKind.Property);
		item.setInsertText(option.getName() + ": " + toYamlValue(option.getDefaultValue()));
		item.setDetail(option.getJavaType());
		if (markdown) {
			item.setDocumentation(CamelOptionNamesCompletionsFuture.getMarkupDocumentation(option));
		} else {
			item.setDocumentation(CamelOptionNamesCompletionsFuture.getDocumentation(option));
		}
		item.setSortText((option.isRequired() ? "0-" : "1-") + option.getName());
		CompletionResolverUtils.applyDeprecation(item, option.isDeprecated());
		return item;
	}

	private static String toYamlValue(Object value) {
		if (value == null) {
			return "";
		}
		String text = value.toString();
		if (text.isEmpty() || YAML_SPECIAL_CHARACTERS.matcher(text).find()) {
			return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
		}
		return text;
	}

	private static class Endpoint {
		private final String scheme;
		private final boolean schemeOnly;
		private final String kameletTemplateId;
		private final boolean producer;
		private final int parametersLine;

		Endpoint(String scheme, boolean schemeOnly, String kameletTemplateId, boolean producer, int parametersLine) {
			this.scheme = scheme;
			this.schemeOnly = schemeOnly;
			this.kameletTemplateId = kameletTemplateId;
			this.producer = producer;
			this.parametersLine = parametersLine;
		}
	}
}
