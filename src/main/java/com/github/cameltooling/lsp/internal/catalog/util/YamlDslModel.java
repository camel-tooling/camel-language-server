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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * The model of the Camel YAML DSL that a Camel catalog provides ({@value #MODEL_RESOURCE}): its nodes, which are the
 * elements of the DSL such as the routes and the EIPs, with the keys they accept, the nodes these keys reference and
 * their documentation.
 * <p>
 * The catalogs of the Camel versions before 4.22 provide no model, the model of the bundled catalog is used instead:
 * {@link #isOwnModel()} is then false, and the EIP models of the catalog better know the options of its version.
 */
public final class YamlDslModel {

	public static final String MODEL_RESOURCE = "org/apache/camel/catalog/schemas/camelYamlDsl-model.json";
	/** The node of the top-level elements of a document. */
	public static final String ROOT = "root";
	/** The kind of the keys whose value is an expression. */
	public static final String KIND_EXPRESSION = "expression";
	/** The labels of the nodes of the EIPs, the languages and the data formats. */
	public static final String LABEL_EIP = "eip";
	public static final String LABEL_LANGUAGE = "language";
	public static final String LABEL_DATA_FORMAT = "dataformat";
	/** The kinds of the keys with a scalar value, the other kinds are elements and expressions. */
	private static final Set<String> SCALAR_KINDS = Set.of("attribute", "value");
	private static final String TYPE_OBJECT = "object";
	private static final String TYPE_ARRAY = "array";

	private static final Logger LOGGER = LoggerFactory.getLogger(YamlDslModel.class);
	private static final Map<CamelCatalog, YamlDslModel> MODELS = Collections.synchronizedMap(new WeakHashMap<>());
	/** The catalog providing the model to the catalogs which provide none. */
	private static final CamelCatalog BUNDLED_CATALOG = new DefaultCamelCatalog(true);

	private Map<String, Node> nodes = new HashMap<>();
	private transient boolean ownModel;

	/**
	 * @return the model of the catalog, without nodes if it cannot be loaded
	 */
	public static YamlDslModel of(CamelCatalog catalog) {
		return MODELS.computeIfAbsent(catalog, YamlDslModel::load);
	}

	private static YamlDslModel load(CamelCatalog catalog) {
		try (InputStream stream = catalog.getVersionManager().getResourceAsStream(MODEL_RESOURCE)) {
			if (stream != null) {
				try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
					YamlDslModel model = new Gson().fromJson(reader, YamlDslModel.class);
					if (model != null && model.nodes != null) {
						model.nodes.forEach((name, node) -> node.name = name);
						model.ownModel = isProvidedBy(catalog);
						return model;
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Cannot load the Camel YAML DSL model of the Camel catalog {}", catalog.getCatalogVersion(), e);
		}
		return new YamlDslModel();
	}

	/**
	 * @return whether the catalog of the loaded Camel version provides the model, the version manager falls back to the
	 * bundled catalog otherwise
	 */
	private static boolean isProvidedBy(CamelCatalog catalog) {
		String version = catalog.getLoadedVersion();
		ClassLoader classLoader = catalog.getVersionManager().getClassLoader();
		if (version == null || classLoader == null) {
			return true;
		}
		try {
			Enumeration<URL> urls = classLoader.getResources(MODEL_RESOURCE);
			while (urls.hasMoreElements()) {
				if (urls.nextElement().toString().contains("/camel-catalog-" + version + ".jar!")) {
					return true;
				}
			}
		} catch (IOException e) {
			LOGGER.debug("Cannot find the Camel YAML DSL model of the Camel catalog {}", version, e);
		}
		return false;
	}

	public boolean isAvailable() {
		return getRoot() != null;
	}

	/**
	 * @return whether the model is the one of the Camel version of the catalog, rather than the one of the bundled catalog
	 */
	public boolean isOwnModel() {
		return ownModel;
	}

	public Node getRoot() {
		return getNode(ROOT);
	}

	/**
	 * @return whether the Camel version of the catalog knows the language, data format or EIP written with this key, which
	 * is the one of the node. With the model of the bundled catalog, the names known by the bundled catalog and not by the
	 * catalog were added after its version. The other keys, such as the json data format, are not names of the catalogs.
	 */
	public boolean isKnownByCatalogVersion(CamelCatalog catalog, String key, Node node) {
		if (ownModel) {
			return true;
		}
		return !(node.hasLabel(LABEL_LANGUAGE) && isAddedAfter(key, BUNDLED_CATALOG.findLanguageNames(), catalog.findLanguageNames())
				|| node.hasLabel(LABEL_DATA_FORMAT) && isAddedAfter(key, BUNDLED_CATALOG.findDataFormatNames(), catalog.findDataFormatNames())
				|| node.hasLabel(LABEL_EIP) && isAddedAfter(key, BUNDLED_CATALOG.findModelNames(), catalog.findModelNames()));
	}

	private static boolean isAddedAfter(String name, List<String> bundledNames, List<String> catalogNames) {
		return bundledNames.contains(name) && !catalogNames.contains(name);
	}

	public Node getNode(String name) {
		return name != null ? nodes.get(name) : null;
	}

	/**
	 * @return the node that the child references, null for a scalar or a free-form value
	 */
	public Node getNode(Child child) {
		return child != null ? getNode(child.ref) : null;
	}

	/**
	 * @return the child of the parent written with this key, null if the model knows no such key. Besides the children
	 * of the parent, it can be the from of a route defined with its from only, or the expression of an element written
	 * with the name of its language.
	 */
	public Child getChild(Node parent, String key) {
		Child child = parent.getChild(key);
		if (child == null && parent == getRoot() && YamlDslKeys.FROM.equals(key)) {
			child = Child.reference(YamlDslKeys.FROM);
		}
		Node languages = child == null ? getLanguages(parent) : null;
		if (languages != null) {
			child = languages.getChild(key);
		}
		return child;
	}

	/**
	 * @return the node listing the languages that the expression of the node can be written with, null if the node has
	 * no expression
	 */
	public Node getLanguages(Node node) {
		Child expression = node.getChild(YamlDslKeys.EXPRESSION);
		return expression != null && KIND_EXPRESSION.equals(expression.kind) ? getNode(expression.ref) : null;
	}

	/**
	 * @return whether the child is a list of mappings with the name of the list as single key, such as the onException
	 * of a route configuration
	 */
	public boolean isWrappedList(Child child) {
		return child.ref == null && TYPE_ARRAY.equals(child.type) && getNode(child.name) != null;
	}

	/**
	 * @return the node of the element at the end of the path of keys from the top level of a document, null if the model
	 * does not know the path
	 */
	public Node resolve(List<String> keys) {
		Node node = getRoot();
		int index = 0;
		while (node != null && index < keys.size()) {
			String key = keys.get(index++);
			Child child = getChild(node, key);
			if (child != null && isWrappedList(child)) {
				node = getNode(key);
				if (index < keys.size() && key.equals(keys.get(index))) {
					index++;
				}
			} else {
				node = getNode(child);
			}
		}
		return node;
	}

	/**
	 * @return whether the node is an endpoint: a URI, configured with free-form parameters
	 */
	public static boolean isEndpoint(Node node) {
		return node.getChild(YamlDslKeys.URI) != null && hasEndpointParameters(node);
	}

	/**
	 * @return whether the node is configured with the free-form parameters of an endpoint, such as a URI or a Kamelet
	 */
	public static boolean hasEndpointParameters(Node node) {
		Child parameters = node.getChild(YamlDslKeys.PARAMETERS);
		return parameters != null && parameters.isFreeForm();
	}

	public static final class Node {
		private transient String name;
		private String title;
		private String description;
		private String label;
		private boolean deprecated;
		private boolean listChildren;
		private List<Child> children = new ArrayList<>();
		private transient Map<String, Child> childrenByName;

		public String getName() {
			return name;
		}

		public String getTitle() {
			return title;
		}

		public String getDescription() {
			return description;
		}

		public String getLabel() {
			return label;
		}

		public boolean hasLabel(String expectedLabel) {
			return label != null && Arrays.asList(label.split(",")).contains(expectedLabel);
		}

		public boolean isDeprecated() {
			return deprecated;
		}

		/**
		 * @return whether the node is a list of single-key mappings, such as the steps of a route, whose keys are its children
		 */
		public boolean isListChildren() {
			return listChildren;
		}

		public List<Child> getChildren() {
			return children;
		}

		public synchronized Child getChild(String childName) {
			if (childrenByName == null) {
				childrenByName = new HashMap<>();
				for (Child child : children) {
					childrenByName.put(child.name, child);
				}
			}
			return childrenByName.get(childName);
		}
	}

	public static final class Child {
		private String name;
		private String type;
		private String ref;
		private String kind;
		private String displayName;
		private String description;
		private boolean required;
		private boolean deprecated;
		private String deprecationNote;
		@SerializedName("enum")
		private List<String> enumValues;
		@SerializedName("default")
		private String defaultValue;

		static Child reference(String name) {
			Child child = new Child();
			child.name = name;
			child.ref = name;
			child.type = TYPE_OBJECT;
			return child;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public String getRef() {
			return ref;
		}

		public String getKind() {
			return kind;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getDescription() {
			return description;
		}

		public boolean isRequired() {
			return required;
		}

		public boolean isDeprecated() {
			return deprecated;
		}

		public String getDeprecationNote() {
			return deprecationNote;
		}

		public List<String> getEnumValues() {
			return enumValues != null ? enumValues : Collections.emptyList();
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		/**
		 * @return whether the value of the child is a scalar, rather than an element or an expression
		 */
		public boolean isScalar() {
			return kind != null ? SCALAR_KINDS.contains(kind) : ref == null && !TYPE_OBJECT.equals(type) && !TYPE_ARRAY.equals(type);
		}

		/**
		 * @return whether the value of the child is written freely, such as the parameters of an endpoint
		 */
		public boolean isFreeForm() {
			return ref == null && TYPE_OBJECT.equals(type);
		}
	}
}
