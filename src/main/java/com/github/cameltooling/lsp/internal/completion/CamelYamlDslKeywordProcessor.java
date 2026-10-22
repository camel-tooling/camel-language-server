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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tooling.model.EipModel;
import org.apache.camel.tooling.model.EipModel.EipOptionModel;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.github.cameltooling.lsp.internal.catalog.util.ModelDocumentationHelper;
import com.github.cameltooling.lsp.internal.catalog.util.YamlDslKeys;
import com.github.cameltooling.lsp.internal.catalog.util.YamlDslModel;
import com.github.cameltooling.lsp.internal.catalog.util.YamlDslModel.Child;
import com.github.cameltooling.lsp.internal.catalog.util.YamlDslModel.Node;

/**
 * Completion and hover for the keys of a Camel YAML DSL document which are not part of an endpoint URI: the EIPs in
 * steps, the options of the EIPs and their values, the expression languages and the data formats.
 * <p>
 * The keys at a position are the ones of the node of the {@link YamlDslModel} reached by following the keys of the
 * document from its top level. The options are documented with the EIP models of the Camel catalog, which also tell
 * the options of the Camel versions whose catalog provides no YAML DSL model.
 */
public class CamelYamlDslKeywordProcessor {

	private static final String ATTRIBUTE_KIND = "attribute";

	private final YamlDslLines lines;
	private final CompletableFuture<CamelCatalog> camelCatalog;
	private final boolean markdown;

	public CamelYamlDslKeywordProcessor(YamlDslLines lines, CompletableFuture<CamelCatalog> camelCatalog, boolean markdown) {
		this.lines = lines;
		this.camelCatalog = camelCatalog;
		this.markdown = markdown;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		int ownerLine = lines.owner(position);
		if (ownerLine < 0 || lines.key(ownerLine) == null || lines.isInsideBlockScalar(position)) {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
		String typed = lines.contentBeforeCursor(position);
		int separator = YamlDslLines.separatorIndex(typed);
		List<String> path = lines.keyPath(ownerLine);
		return camelCatalog.thenApply(catalog -> {
			YamlDslModel model = YamlDslModel.of(catalog);
			Node owner = findNode(model, path);
			if (owner == null) {
				return Collections.emptyList();
			}
			if (separator > 0) {
				return getValueCompletions(catalog, model, owner, typed.substring(0, separator).trim());
			}
			String filter = typed.toLowerCase(Locale.ROOT);
			return getKeyCompletions(catalog, model, owner).stream()
					.filter(item -> item.getLabel().toLowerCase(Locale.ROOT).startsWith(filter))
					.collect(Collectors.toList());
		});
	}

	public CompletableFuture<Hover> getHover(Position position) {
		int lineNumber = position.getLine();
		String key = lines.key(lineNumber);
		int keyStart = lines.contentColumn(lineNumber);
		int keyEnd = lines.keyEndColumn(lineNumber);
		if (key == null || position.getCharacter() < keyStart || position.getCharacter() >= keyEnd || lines.isInsideBlockScalar(position)) {
			return CompletableFuture.completedFuture(null);
		}
		int ownerLine = lines.owner(position);
		List<String> path = ownerLine >= 0 ? lines.keyPath(ownerLine) : Collections.emptyList();
		Range range = new Range(new Position(lineNumber, keyStart), new Position(lineNumber, keyEnd));
		return camelCatalog.thenApply(catalog -> {
			YamlDslModel model = YamlDslModel.of(catalog);
			Node owner = findNode(model, path);
			if (owner != null) {
				EipOptionModel option = findOption(catalog.eipModel(owner.getName()), key);
				if (option != null) {
					return new Hover(ModelDocumentationHelper.getOptionDocumentation(key, option, markdown), range);
				}
				Child child = model.getChild(owner, key);
				Node node = model.getNode(child);
				if (node != null) {
					return new Hover(getDocumentation(catalog, node), range);
				} else if (child != null) {
					return new Hover(ModelDocumentationHelper.getOptionDocumentation(key, toOption(child), markdown), range);
				}
			}
			EipModel eipModel = catalog.eipModel(key);
			return eipModel != null ? new Hover(ModelDocumentationHelper.getModelDocumentation(eipModel, markdown), range) : null;
		});
	}

	/**
	 * @return the node owning the mapping at the end of the path: the node reached by following the path from the top
	 * level, else the node named like the last key of the path, for the documents that the model cannot follow
	 */
	private static Node findNode(YamlDslModel model, List<String> path) {
		if (!model.isAvailable() || path.isEmpty()) {
			return null;
		}
		List<String> dslPath = getDslPath(path);
		Node node = dslPath.isEmpty() ? null : model.resolve(dslPath);
		return node != null ? node : model.getNode(path.get(path.size() - 1));
	}

	/**
	 * @return the path in the Camel YAML DSL: in a Kubernetes resource, the path from the routes of its spec
	 */
	private static List<String> getDslPath(List<String> path) {
		if (path.size() >= 2 && YamlDslKeys.SPEC.equals(path.get(0))) {
			String routes = path.get(1);
			boolean routesKey = YamlDslKeys.TEMPLATE.equals(routes) || YamlDslKeys.FLOW.equals(routes) || YamlDslKeys.FLOWS.equals(routes);
			return routesKey ? path.subList(2, path.size()) : Collections.emptyList();
		}
		return path;
	}

	private List<CompletionItem> getKeyCompletions(CamelCatalog catalog, YamlDslModel model, Node owner) {
		if (owner.isListChildren()) {
			return getElementCompletions(catalog, model, owner);
		}
		EipModel eipModel = catalog.eipModel(owner.getName());
		List<CompletionItem> items = new ArrayList<>();
		for (Child child : owner.getChildren()) {
			EipOptionModel option = findOption(eipModel, child.getName());
			Node node = model.getNode(child);
			if (isUnknownToCatalogVersion(catalog, model, eipModel, child, option, node)) {
				continue;
			}
			items.add(createChildItem(catalog, child, option, node));
			if (YamlDslModel.KIND_EXPRESSION.equals(child.getKind()) && node != null) {
				// the expression can be written either in its key or directly with the name of its language
				items.addAll(getElementCompletions(catalog, model, node));
			}
		}
		if (!model.isOwnModel() && eipModel != null) {
			// the options removed after the Camel version of the catalog
			for (EipOptionModel removedOption : eipModel.getOptions()) {
				if (ATTRIBUTE_KIND.equals(removedOption.getKind()) && owner.getChild(removedOption.getName()) == null) {
					items.add(createOptionItem(removedOption.getName(), removedOption));
				}
			}
		}
		return items;
	}

	/**
	 * @return whether the model, which is not the one of the Camel version of the catalog, has a key this version does not know
	 */
	private static boolean isUnknownToCatalogVersion(CamelCatalog catalog, YamlDslModel model, EipModel eipModel, Child child, EipOptionModel option, Node node) {
		if (model.isOwnModel()) {
			return false;
		}
		if (child.isScalar()) {
			return eipModel != null && option == null;
		}
		return node != null && !model.isKnownByCatalogVersion(catalog, child.getName(), node);
	}

	/**
	 * @return the elements of a list of single-key mappings, such as the EIPs of steps, or of the languages of an expression
	 */
	private List<CompletionItem> getElementCompletions(CamelCatalog catalog, YamlDslModel model, Node list) {
		List<CompletionItem> items = new ArrayList<>();
		for (Child child : list.getChildren()) {
			Node node = model.getNode(child);
			if (node == null) {
				continue;
			}
			if (model.isKnownByCatalogVersion(catalog, child.getName(), node)) {
				items.add(createNodeItem(catalog, child.getName(), node, list.isListChildren() ? CompletionItemKind.Keyword : CompletionItemKind.Value));
			}
		}
		if (!model.isOwnModel() && YamlDslKeys.STEPS.equals(list.getName())) {
			// the EIPs removed after the Camel version of the catalog, the model knows the other ones
			for (String name : catalog.findModelNames()) {
				EipModel eipModel = catalog.eipModel(name);
				if (eipModel != null && model.getNode(name) == null && hasLabel(eipModel.getLabel(), YamlDslModel.LABEL_EIP)) {
					items.add(createModelItem(name, eipModel, CompletionItemKind.Keyword));
				}
			}
		}
		return items;
	}

	private List<CompletionItem> getValueCompletions(CamelCatalog catalog, YamlDslModel model, Node owner, String key) {
		EipOptionModel option = findOption(catalog.eipModel(owner.getName()), key);
		Child child = model.getChild(owner, key);
		if (option == null && (child == null || !child.isScalar())) {
			return Collections.emptyList();
		}
		List<String> enums = option != null ? option.getEnums() : child.getEnumValues();
		String type = option != null ? option.getType() : child.getType();
		List<String> values;
		if (enums != null && !enums.isEmpty()) {
			values = enums;
		} else if ("boolean".equals(type)) {
			values = List.of("true", "false");
		} else {
			return Collections.emptyList();
		}
		Object defaultValue = option != null ? option.getDefaultValue() : child.getDefaultValue();
		return createValueItems(values, defaultValue != null ? defaultValue.toString() : null);
	}

	static List<CompletionItem> createValueItems(List<String> values, String defaultValue) {
		List<CompletionItem> items = new ArrayList<>();
		for (String value : values) {
			CompletionItem item = new CompletionItem(value);
			item.setKind(CompletionItemKind.EnumMember);
			boolean isDefault = value.equals(defaultValue);
			if (isDefault) {
				item.setDetail("Default value");
			}
			item.setSortText((isDefault ? "0-" : "1-") + value);
			items.add(item);
		}
		return items;
	}

	private static EipOptionModel findOption(EipModel eipModel, String name) {
		if (eipModel == null) {
			return null;
		}
		return eipModel.getOptions().stream().filter(option -> name.equals(option.getName())).findFirst().orElse(null);
	}

	private CompletionItem createChildItem(CamelCatalog catalog, Child child, EipOptionModel option, Node node) {
		if (option != null) {
			return createOptionItem(child.getName(), option);
		} else if (node != null && child.getDescription() == null) {
			boolean value = node.hasLabel(YamlDslModel.LABEL_LANGUAGE) || node.hasLabel(YamlDslModel.LABEL_DATA_FORMAT);
			return createNodeItem(catalog, child.getName(), node, value ? CompletionItemKind.Value : CompletionItemKind.Property);
		}
		return createOptionItem(child.getName(), toOption(child));
	}

	private CompletionItem createNodeItem(CamelCatalog catalog, String key, Node node, CompletionItemKind kind) {
		EipModel eipModel = catalog.eipModel(node.getName());
		return createModelItem(key, eipModel != null ? eipModel : toEipModel(node), kind);
	}

	private CompletionItem createModelItem(String key, EipModel eipModel, CompletionItemKind kind) {
		CompletionItem item = createKeyItem(key, eipModel.getTitle());
		item.setKind(kind);
		setDocumentation(item, ModelDocumentationHelper.getModelDocumentation(eipModel, markdown));
		CompletionResolverUtils.applyDeprecation(item, eipModel.isDeprecated());
		return item;
	}

	private CompletionItem createOptionItem(String key, EipOptionModel option) {
		CompletionItem item = createKeyItem(key, option.getJavaType() != null ? option.getJavaType() : option.getType());
		setDocumentation(item, ModelDocumentationHelper.getOptionDocumentation(key, option, markdown));
		item.setSortText((option.isRequired() ? "0-" : "1-") + key);
		CompletionResolverUtils.applyDeprecation(item, option.isDeprecated());
		return item;
	}

	private CompletionItem createKeyItem(String key, String detail) {
		CompletionItem item = new CompletionItem(key);
		item.setKind(CompletionItemKind.Property);
		item.setInsertText(key + ": ");
		item.setDetail(detail);
		return item;
	}

	private MarkupContent getDocumentation(CamelCatalog catalog, Node node) {
		EipModel eipModel = catalog.eipModel(node.getName());
		return ModelDocumentationHelper.getModelDocumentation(eipModel != null ? eipModel : toEipModel(node), markdown);
	}

	private void setDocumentation(CompletionItem item, MarkupContent documentation) {
		if (markdown) {
			item.setDocumentation(documentation);
		} else {
			item.setDocumentation(documentation.getValue());
		}
	}

	private static boolean hasLabel(String labels, String expectedLabel) {
		return labels != null && List.of(labels.split(",")).contains(expectedLabel);
	}

	private static EipOptionModel toOption(Child child) {
		EipOptionModel option = new EipOptionModel();
		option.setName(child.getName());
		option.setDisplayName(child.getDisplayName());
		option.setKind(child.getKind());
		option.setType(child.getType());
		option.setDescription(child.getDescription());
		option.setRequired(child.isRequired());
		option.setDeprecated(child.isDeprecated());
		option.setDeprecationNote(child.getDeprecationNote());
		option.setDefaultValue(child.getDefaultValue());
		option.setEnums(child.getEnumValues().isEmpty() ? null : child.getEnumValues());
		return option;
	}

	private static EipModel toEipModel(Node node) {
		EipModel eipModel = new EipModel();
		eipModel.setName(node.getName());
		eipModel.setTitle(node.getTitle());
		eipModel.setDescription(node.getDescription());
		eipModel.setLabel(node.getLabel());
		eipModel.setDeprecated(node.isDeprecated());
		return eipModel;
	}
}
