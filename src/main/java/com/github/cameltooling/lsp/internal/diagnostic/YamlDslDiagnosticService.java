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
package com.github.cameltooling.lsp.internal.diagnostic;

import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.tooling.model.EipModel;
import org.apache.camel.tooling.model.EipModel.EipOptionModel;
import org.apache.camel.util.StringHelper;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DiagnosticTag;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import com.github.cameltooling.lsp.internal.catalog.diagnostic.BooleanErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.CamelDiagnosticMessage;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.EnumErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.IntegerErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.InvalidDurationErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.NumberErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.ReferenceErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.RequiredErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.UnknownComponentErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.diagnostic.UnknownErrorMsg;
import com.github.cameltooling.lsp.internal.catalog.util.YamlDslKeys;
import com.github.cameltooling.lsp.internal.catalog.util.YamlDslModel;
import com.github.cameltooling.lsp.internal.catalog.util.YamlDslModel.Child;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperFactory;

/**
 * Validates the structure and the endpoints of Camel YAML DSL documents.
 * <p>
 * The structure is validated with the YAML DSL model of the Camel catalog: unknown elements, steps and options, missing
 * required options, invalid enum values and deprecated elements. The endpoints, including the options written in their
 * {@code parameters}, are validated with the Camel catalog like the endpoints of the XML and Java DSLs.
 * <p>
 * The catalogs of the Camel versions before 4.22 provide no model, the model of the bundled catalog is used instead.
 * The steps are then the ones of the Camel version of the catalog, and the keys that this version knows and that the
 * model no longer knows are accepted: the options of its EIP models, the elements these options list and its languages.
 */
public class YamlDslDiagnosticService extends DiagnosticService {

	private static final Pattern SCHEME = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*");
	/** Durations with milliseconds, accepted by Camel but not by the Camel catalog validation. */
	private static final Pattern DURATION_WITH_MILLISECONDS = Pattern.compile("(\\d+(ms|[dhms]))+");
	private static final String ATTRIBUTE_KIND = "attribute";
	private static final Set<String> SCALAR_TYPES = Set.of("string", "enum", "boolean", "number", "integer");

	public YamlDslDiagnosticService(CompletableFuture<CamelCatalog> camelCatalog) {
		super(camelCatalog);
	}

	public List<Diagnostic> compute(String camelText, TextDocumentItem documentItem) {
		if (documentItem == null || !new ParserFileHelperFactory().isCamelYamlDSL(documentItem, documentItem.getUri())) {
			return Collections.emptyList();
		}
		List<Diagnostic> diagnostics = new ArrayList<>();
		try {
			CamelCatalog catalog = camelCatalog.get();
			for (Node document : new Yaml(new SafeConstructor(new LoaderOptions())).composeAll(new StringReader(camelText))) {
				new DocumentValidator(catalog, diagnostics).validateDocument(document);
			}
		} catch (YAMLException e) {
			// The YAML syntax errors are reported by the YAML tooling, the documents before the error are validated
			LOGGER.debug("Cannot parse the YAML document {}", documentItem.getUri(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logExceptionValidatingDocument(documentItem.getUri(), e);
		} catch (ExecutionException | RuntimeException e) {
			// do not prevent the publication of the diagnostics computed by the other services
			logExceptionValidatingDocument(documentItem.getUri(), e);
		}
		return diagnostics;
	}

	private static class DocumentValidator {

		private final CamelCatalog catalog;
		private final YamlDslModel model;
		private final List<Diagnostic> diagnostics;

		DocumentValidator(CamelCatalog catalog, List<Diagnostic> diagnostics) {
			this.catalog = catalog;
			this.model = YamlDslModel.of(catalog);
			this.diagnostics = diagnostics;
		}

		void validateDocument(Node document) {
			if (document instanceof SequenceNode) {
				validateRoutes(document);
			} else if (document instanceof MappingNode) {
				// Kubernetes custom resources
				String kind = getScalarValue(getValue(document, YamlDslKeys.KIND));
				Node spec = getValue(document, YamlDslKeys.SPEC);
				if (YamlDslKeys.KIND_KAMELET.equals(kind)) {
					validateRoutes(getValue(spec, YamlDslKeys.TEMPLATE));
					validateRoutes(getValue(spec, YamlDslKeys.FLOW));
				} else if (YamlDslKeys.KIND_PIPE.equals(kind) || YamlDslKeys.KIND_KAMELET_BINDING.equals(kind)) {
					validatePipe(spec);
				} else if (YamlDslKeys.KIND_INTEGRATION.equals(kind)) {
					validateRoutes(getValue(spec, YamlDslKeys.FLOWS));
				}
			}
		}

		/**
		 * Validates the top-level elements: a list of elements, or a mapping such as the template of a Kamelet.
		 */
		private void validateRoutes(Node node) {
			YamlDslModel.Node root = model.getRoot();
			if (root == null) {
				return;
			}
			if (node instanceof SequenceNode) {
				validateList(root, node);
			} else if (node instanceof MappingNode) {
				validateListItem(root, (MappingNode) node);
			}
		}

		/**
		 * Validates a list of single-key mappings, such as the steps of a route.
		 */
		private void validateList(YamlDslModel.Node list, Node value) {
			if (value instanceof SequenceNode) {
				for (Node item : ((SequenceNode) value).getValue()) {
					if (item instanceof MappingNode) {
						validateListItem(list, (MappingNode) item);
					}
				}
			}
		}

		private void validateListItem(YamlDslModel.Node list, MappingNode item) {
			for (NodeTuple entry : item.getValue()) {
				String key = getScalarValue(entry.getKeyNode());
				if (key == null) {
					continue;
				}
				Child child = model.getChild(list, key);
				if (YamlDslKeys.STEPS.equals(list.getName())) {
					if (!validateStepName(key, entry.getKeyNode(), child)) {
						// the elements of a step that the Camel version of the catalog does not know cannot be validated
						continue;
					}
				} else if (child != null) {
					checkDeprecation(key, child, null, entry.getKeyNode());
				} else if (model.isOwnModel() || !catalog.findModelNames().contains(key)) {
					addWarning(entry.getKeyNode(), String.format("Unknown Camel YAML DSL element: '%s'", key));
				}
				if (child != null) {
					validateValue(child, entry.getKeyNode(), entry.getValueNode());
				}
			}
		}

		private void validateValue(Child child, Node keyNode, Node value) {
			if (isNull(value)) {
				return;
			}
			if (model.isWrappedList(child)) {
				validateWrappedList(child, value);
			} else {
				validateNode(child, model.getNode(child), keyNode, value);
			}
		}

		/**
		 * Validates the node when it is an endpoint, then its elements.
		 *
		 * @param child the key of the node
		 * @param anchor the node on which the missing options are reported
		 */
		private void validateNode(Child child, YamlDslModel.Node node, Node anchor, Node value) {
			if (node == null) {
				// scalar or free-form value, such as the parameters of an endpoint
				return;
			}
			if (node.isListChildren()) {
				validateList(node, value);
			} else if (value instanceof SequenceNode) {
				// a list of elements of the node, such as the cases of a switch
				for (Node item : ((SequenceNode) value).getValue()) {
					validateNode(child, node, getFirstKey(item), item);
				}
			} else {
				if (YamlDslModel.isEndpoint(node)) {
					validateEndpoint(value, YamlDslKeys.PARAMETERS);
				}
				validateMapping(child, node, anchor, value);
			}
		}

		private void validateMapping(Child child, YamlDslModel.Node node, Node anchor, Node value) {
			if (!(value instanceof MappingNode) || node.getChildren().isEmpty()) {
				// scalar shorthand, for instance to: "log:info", or free-form value
				return;
			}
			String name = child.getName();
			EipModel eipModel = catalog.eipModel(node.getName());
			// the keys of an expression are languages, the keys of an element with an expression can be its language
			YamlDslModel.Node languages = YamlDslModel.KIND_EXPRESSION.equals(child.getKind()) ? node : model.getLanguages(node);
			Set<String> definedKeys = new HashSet<>();
			boolean unknownKeys = false;
			boolean olderVersionKeys = false;
			for (NodeTuple entry : ((MappingNode) value).getValue()) {
				String writtenKey = getScalarValue(entry.getKeyNode());
				if (writtenKey == null) {
					continue;
				}
				// like the YAML DSL deserializers, accept the options in kebab case but not the names of the steps
				String key = StringHelper.dashToCamelCase(writtenKey);
				Child keyChild = model.getChild(node, key);
				definedKeys.add(languages != null && languages.getChild(key) != null ? YamlDslKeys.EXPRESSION : key);
				if (keyChild != null) {
					EipOptionModel option = findOption(eipModel, key);
					checkDeprecation(writtenKey, keyChild, option, entry.getKeyNode());
					checkEnumValue(writtenKey, keyChild, option, entry.getValueNode());
					validateValue(keyChild, entry.getKeyNode(), entry.getValueNode());
				} else if (!model.isOwnModel() && isKeyOfCatalogVersion(node, eipModel, languages, key)) {
					olderVersionKeys = true;
				} else {
					unknownKeys = true;
					addWarning(entry.getKeyNode(), String.format("Unknown option '%s' for '%s'", writtenKey, name));
				}
			}
			// the keys removed after the Camel version of the catalog can stand for the required ones of the model, for
			// instance the toType of transform, replaced in 4.18 by the transformDataType EIP, for its expression
			if (!olderVersionKeys) {
				for (Child option : node.getChildren()) {
					if (option.isRequired() && !definedKeys.contains(option.getName()) && isCheckedWhenRequired(option, eipModel, unknownKeys)) {
						addWarning(anchor, String.format("Missing required option '%s' for '%s'", option.getName(), name));
					}
				}
			}
		}

		/**
		 * @return whether the Camel version of the catalog knows the key that the model, of a more recent version, does
		 * not know: an option of the EIP model of the node, an element listed by an option like the elements of the node,
		 * such as the data formats of marshal, or a language
		 */
		private boolean isKeyOfCatalogVersion(YamlDslModel.Node node, EipModel eipModel, YamlDslModel.Node languages, String key) {
			if (languages != null && catalog.findLanguageNames().contains(key)) {
				return true;
			}
			if (eipModel != null) {
				for (EipOptionModel option : eipModel.getOptions()) {
					List<String> elements = option.getOneOfs();
					if (key.equals(option.getName()) && ATTRIBUTE_KIND.equals(option.getKind())
							|| elements != null && elements.contains(key) && elements.stream().anyMatch(element -> node.getChild(element) != null)) {
						return true;
					}
				}
			}
			return false;
		}

		/**
		 * The required elements are not checked, as the model marks elements as required even when they have alternatives,
		 * for instance the route of a route template which can be defined with its from only. With the model of a more
		 * recent version, the options are checked when the EIP model of the Camel version of the catalog requires them.
		 */
		private boolean isCheckedWhenRequired(Child child, EipModel eipModel, boolean unknownKeys) {
			if (!model.isOwnModel() && eipModel != null) {
				EipOptionModel option = findOption(eipModel, child.getName());
				if (option == null || !option.isRequired()) {
					return false;
				}
			}
			if (YamlDslModel.KIND_EXPRESSION.equals(child.getKind())) {
				// an unknown key is likely the expression, with a misspelled language
				return !unknownKeys;
			}
			return child.isScalar() && SCALAR_TYPES.contains(child.getType());
		}

		/**
		 * @param option the option of the EIP model of the Camel version of the catalog, which knows the values of its version
		 */
		private void checkEnumValue(String key, Child child, EipOptionModel option, Node value) {
			List<String> values = option != null && option.getEnums() != null && !option.getEnums().isEmpty() ? option.getEnums() : child.getEnumValues();
			String text = getScalarValue(value);
			if (values.isEmpty() || text == null || isNull(value) || text.contains("{{") || text.contains("${")) {
				return;
			}
			if (values.stream().noneMatch(text::equalsIgnoreCase)) {
				addWarning(value, String.format("Invalid value '%s' for '%s', the possible values are: %s", text, key, String.join(", ", values)));
			}
		}

		private void checkDeprecation(String key, Child child, EipOptionModel option, Node keyNode) {
			YamlDslModel.Node node = model.getNode(child);
			if (option != null ? option.isDeprecated() : child.isDeprecated() || node != null && node.isDeprecated()) {
				addDeprecation(keyNode, key);
			}
		}

		/**
		 * Validates a list of mappings with the name of the list as single key, such as the onException of a route configuration.
		 */
		private void validateWrappedList(Child child, Node value) {
			String name = child.getName();
			if (value instanceof SequenceNode) {
				for (Node item : ((SequenceNode) value).getValue()) {
					if (item instanceof MappingNode) {
						for (NodeTuple entry : ((MappingNode) item).getValue()) {
							String key = getScalarValue(entry.getKeyNode());
							if (name.equals(key)) {
								if (!isNull(entry.getValueNode())) {
									validateNode(child, model.getNode(name), entry.getKeyNode(), entry.getValueNode());
								}
							} else if (key != null) {
								addWarning(entry.getKeyNode(), String.format("Unknown option '%s' for '%s'", key, name));
							}
						}
					}
				}
			}
		}

		/**
		 * Reports the steps that the Camel version of the catalog does not know, and the deprecated ones. The catalog
		 * finds the names of its version only, the models of the other names are the ones of the bundled catalog.
		 *
		 * @return whether the Camel version of the catalog knows the step
		 */
		private boolean validateStepName(String name, Node keyNode, Child child) {
			boolean known = model.isOwnModel() ? child != null : catalog.findModelNames().contains(name);
			EipModel eipModel = known ? catalog.eipModel(name) : null;
			if (!known) {
				addWarning(keyNode, String.format("Unknown Camel EIP or step: '%s'", name));
			} else if (eipModel != null && eipModel.isDeprecated()) {
				addDeprecation(keyNode, name);
			}
			return known;
		}

		private void validatePipe(Node spec) {
			validatePipeEndpoint(getValue(spec, YamlDslKeys.SOURCE));
			validatePipeEndpoint(getValue(spec, YamlDslKeys.SINK));
			Node steps = getValue(spec, YamlDslKeys.STEPS);
			if (steps instanceof SequenceNode) {
				for (Node step : ((SequenceNode) steps).getValue()) {
					validatePipeEndpoint(step);
				}
			}
		}

		private void validatePipeEndpoint(Node endpoint) {
			// an endpoint of a Pipe either references a Kubernetes resource such as a Kamelet, or has a URI
			if (getValue(endpoint, YamlDslKeys.URI) != null) {
				validateEndpoint(endpoint, YamlDslKeys.PROPERTIES);
			}
		}

		/**
		 * @param endpoint an endpoint, either as a URI or as a mapping with a URI and the parameters of the URI
		 * @param parametersKey the key of the parameters of the URI
		 */
		private void validateEndpoint(Node endpoint, String parametersKey) {
			Node uriNode = endpoint instanceof ScalarNode ? endpoint : getValue(endpoint, YamlDslKeys.URI);
			if (!(uriNode instanceof ScalarNode) || isNull(uriNode)) {
				return;
			}
			ScalarNode uriScalar = (ScalarNode) uriNode;
			String uri = uriScalar.getValue().trim();
			int pathStart = uri.indexOf(':');
			String scheme = pathStart >= 0 ? uri.substring(0, pathStart) : uri;
			if (uri.contains("${") || !SCHEME.matcher(scheme).matches()) {
				// dynamic URI or property placeholder in the scheme, cannot be validated
				return;
			}
			if (catalog.componentJSonSchema(scheme) == null) {
				addDiagnostic(getRange(uriScalar), new UnknownComponentErrorMsg().getErrorMessage(scheme), DiagnosticSeverity.Error, null);
				return;
			}
			Map<String, NodeTuple> parameters = getScalarParameters(getValue(endpoint, parametersKey));
			String endpointUri = createEndpointUri(scheme, uri, pathStart < 0, parameters);
			if (endpointUri == null) {
				return;
			}
			EndpointValidationResult result = catalog.validateEndpointProperties(endpointUri, false);
			if (result.hasErrors() && result.getIncapable() == null) {
				addDiagnostics(result, PlaceholderErrors.of(catalog, endpointUri, result), uriScalar, parameters);
			}
		}

		private String createEndpointUri(String scheme, String uri, boolean schemeOnly, Map<String, NodeTuple> parameters) {
			if (parameters.isEmpty()) {
				return uri;
			}
			try {
				Map<String, String> options = new LinkedHashMap<>();
				if (!schemeOnly) {
					options.putAll(catalog.endpointProperties(uri));
				}
				parameters.forEach((name, parameter) -> options.put(name, ((ScalarNode) parameter.getValueNode()).getValue()));
				return catalog.asEndpointUri(scheme, options, false);
			} catch (URISyntaxException | RuntimeException e) {
				LOGGER.debug("Cannot create the endpoint URI from {} and its parameters", uri, e);
				return null;
			}
		}

		private void addDiagnostics(EndpointValidationResult result, PlaceholderErrors placeholderErrors, ScalarNode uri, Map<String, NodeTuple> parameters) {
			if (result.getUnknown() != null) {
				for (String option : result.getUnknown()) {
					if (placeholderErrors.isUnknownOption(option)) {
						continue;
					}
					addDiagnostic(getOptionRange(option, uri, parameters, true), new UnknownErrorMsg().getErrorMessage(option), DiagnosticSeverity.Error, ERROR_CODE_UNKNOWN_PROPERTIES);
				}
			}
			if (result.getInvalidEnum() != null) {
				for (Entry<String, String> invalidEnum : result.getInvalidEnum().entrySet()) {
					if (placeholderErrors.isInvalidEnum(invalidEnum.getKey())) {
						continue;
					}
					addDiagnostic(getOptionRange(invalidEnum.getKey(), uri, parameters, false), new EnumErrorMsg().getErrorMessage(result, invalidEnum), DiagnosticSeverity.Error, ERROR_CODE_INVALID_ENUM);
				}
			}
			addInvalidValueDiagnostics(result.getInvalidBoolean(), new BooleanErrorMsg(), uri, parameters);
			addInvalidValueDiagnostics(result.getInvalidInteger(), new IntegerErrorMsg(), uri, parameters);
			addInvalidValueDiagnostics(result.getInvalidNumber(), new NumberErrorMsg(), uri, parameters);
			addInvalidValueDiagnostics(result.getInvalidReference(), new ReferenceErrorMsg(), uri, parameters);
			if (result.getInvalidDuration() != null) {
				Map<String, String> invalidDurations = new LinkedHashMap<>(result.getInvalidDuration());
				invalidDurations.values().removeIf(value -> DURATION_WITH_MILLISECONDS.matcher(value).matches());
				addInvalidValueDiagnostics(invalidDurations, new InvalidDurationErrorMsg(), uri, parameters);
			}
			if (result.getRequired() != null && !result.getRequired().isEmpty()) {
				addDiagnostic(getRange(uri), new RequiredErrorMsg().getErrorMessage(result.getRequired()), DiagnosticSeverity.Error, null);
			}
			if (result.getSyntaxError() != null) {
				addDiagnostic(getRange(uri), result.getSyntaxError(), DiagnosticSeverity.Error, null);
			}
		}

		private void addInvalidValueDiagnostics(Map<String, String> invalidValues, CamelDiagnosticMessage<Entry<String, String>> message, ScalarNode uri, Map<String, NodeTuple> parameters) {
			if (invalidValues != null) {
				for (Entry<String, String> invalidValue : invalidValues.entrySet()) {
					addDiagnostic(getOptionRange(invalidValue.getKey(), uri, parameters, false), message.getErrorMessage(invalidValue), DiagnosticSeverity.Error, null);
				}
			}
		}

		/**
		 * @return the range of the name or of the value of the option, in the parameters or in the URI
		 */
		private Range getOptionRange(String option, ScalarNode uri, Map<String, NodeTuple> parameters, boolean name) {
			NodeTuple parameter = parameters.get(option);
			if (parameter != null) {
				return getRange(name ? parameter.getKeyNode() : parameter.getValueNode());
			}
			Range uriRange = getRange(uri);
			if (uriRange.getStart().getLine() == uriRange.getEnd().getLine()) {
				String value = uri.getValue();
				int optionStart = indexOfOption(value, option);
				if (optionStart >= 0) {
					int start = optionStart + (name ? 0 : option.length() + 1);
					int end = name ? optionStart + option.length() : indexOfOptionEnd(value, start);
					int column = uriRange.getStart().getCharacter();
					return new Range(new Position(uriRange.getStart().getLine(), column + start), new Position(uriRange.getStart().getLine(), column + end));
				}
			}
			return uriRange;
		}

		/**
		 * @return the index of the name of the option in the query of the URI, or -1
		 */
		private int indexOfOption(String uri, String option) {
			int queryStart = uri.indexOf('?');
			for (int index = uri.indexOf(option + "=", queryStart); queryStart >= 0 && index > queryStart; index = uri.indexOf(option + "=", index + 1)) {
				char previous = uri.charAt(index - 1);
				if (previous == '?' || previous == '&') {
					return index;
				}
			}
			return -1;
		}

		private int indexOfOptionEnd(String uri, int optionValueStart) {
			int end = uri.indexOf('&', optionValueStart);
			return end >= 0 ? end : uri.length();
		}

		private void addWarning(Node node, String message) {
			addDiagnostic(getRange(node), message, DiagnosticSeverity.Warning, null);
		}

		private void addDeprecation(Node keyNode, String name) {
			addDiagnostic(getRange(keyNode), String.format("'%s' is deprecated", name), DiagnosticSeverity.Hint, null).setTags(List.of(DiagnosticTag.Deprecated));
		}

		private Diagnostic addDiagnostic(Range range, String message, DiagnosticSeverity severity, String code) {
			Diagnostic diagnostic = new Diagnostic(range, message, severity, APACHE_CAMEL_VALIDATION, code);
			diagnostics.add(diagnostic);
			return diagnostic;
		}
	}

	private static EipOptionModel findOption(EipModel eipModel, String name) {
		if (eipModel == null) {
			return null;
		}
		return eipModel.getOptions().stream().filter(option -> name.equals(option.getName())).findFirst().orElse(null);
	}

	/**
	 * @return the first key of a mapping, else the node
	 */
	private static Node getFirstKey(Node item) {
		if (item instanceof MappingNode && !((MappingNode) item).getValue().isEmpty()) {
			return ((MappingNode) item).getValue().get(0).getKeyNode();
		}
		return item;
	}

	/**
	 * @return the parameters with a scalar value, by name
	 */
	private static Map<String, NodeTuple> getScalarParameters(Node parameters) {
		Map<String, NodeTuple> scalarParameters = new LinkedHashMap<>();
		if (parameters instanceof MappingNode) {
			for (NodeTuple parameter : ((MappingNode) parameters).getValue()) {
				String name = getScalarValue(parameter.getKeyNode());
				if (name != null && parameter.getValueNode() instanceof ScalarNode && !isNull(parameter.getValueNode())) {
					scalarParameters.put(name, parameter);
				}
			}
		}
		return scalarParameters;
	}

	private static Node getValue(Node node, String key) {
		if (node instanceof MappingNode) {
			for (NodeTuple entry : ((MappingNode) node).getValue()) {
				if (key.equals(getScalarValue(entry.getKeyNode()))) {
					return entry.getValueNode();
				}
			}
		}
		return null;
	}

	private static String getScalarValue(Node node) {
		return node instanceof ScalarNode ? ((ScalarNode) node).getValue() : null;
	}

	private static boolean isNull(Node node) {
		return Tag.NULL.equals(node.getTag());
	}

	/**
	 * @return the range of the node, without the quotes of a quoted scalar
	 */
	private static Range getRange(Node node) {
		Mark start = node.getStartMark();
		Mark end = node.getEndMark();
		int quote = 0;
		if (node instanceof ScalarNode) {
			ScalarStyle style = ((ScalarNode) node).getScalarStyle();
			quote = style == ScalarStyle.DOUBLE_QUOTED || style == ScalarStyle.SINGLE_QUOTED ? 1 : 0;
		}
		return new Range(new Position(start.getLine(), start.getColumn() + quote), new Position(end.getLine(), end.getColumn() - quote));
	}
}
