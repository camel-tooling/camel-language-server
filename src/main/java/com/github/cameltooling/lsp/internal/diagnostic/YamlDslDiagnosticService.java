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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.tooling.model.EipModel;
import org.apache.camel.util.StringHelper;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
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
 * Validates the endpoints and the step names of Camel YAML DSL documents.
 * <p>
 * The endpoints, including the options written in their {@code parameters}, are validated with the Camel catalog
 * like the endpoints of the XML and Java DSLs. The steps must be known Camel models. The elements of the documents,
 * and the ones which are endpoints, are found with the YAML DSL model of the Camel catalog.
 */
public class YamlDslDiagnosticService extends DiagnosticService {

	private static final Pattern SCHEME = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*");
	/** Durations with milliseconds, accepted by Camel but not by the Camel catalog validation. */
	private static final Pattern DURATION_WITH_MILLISECONDS = Pattern.compile("(\\d+(ms|[dhms]))+");

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
					validateStepName(key, entry.getKeyNode(), child);
				}
				if (child != null) {
					validateValue(child, entry.getValueNode());
				}
			}
		}

		private void validateValue(Child child, Node value) {
			if (isNull(value)) {
				return;
			}
			if (model.isWrappedList(child)) {
				validateWrappedList(child.getName(), value);
			} else {
				validateNode(model.getNode(child), value);
			}
		}

		/**
		 * Validates the node when it is an endpoint, then its elements.
		 */
		private void validateNode(YamlDslModel.Node node, Node value) {
			if (node == null) {
				// scalar or free-form value, such as the parameters of an endpoint
				return;
			}
			if (node.isListChildren()) {
				validateList(node, value);
			} else if (value instanceof SequenceNode) {
				// a list of elements of the node, such as the cases of a switch
				for (Node item : ((SequenceNode) value).getValue()) {
					validateNode(node, item);
				}
			} else {
				if (YamlDslModel.isEndpoint(node)) {
					validateEndpoint(value, YamlDslKeys.PARAMETERS);
				}
				validateMapping(node, value);
			}
		}

		private void validateMapping(YamlDslModel.Node node, Node value) {
			if (value instanceof MappingNode) {
				for (NodeTuple entry : ((MappingNode) value).getValue()) {
					String key = getScalarValue(entry.getKeyNode());
					// like the YAML DSL deserializers, accept the options in kebab case
					Child child = key != null ? model.getChild(node, StringHelper.dashToCamelCase(key)) : null;
					if (child != null) {
						validateValue(child, entry.getValueNode());
					}
				}
			}
		}

		/**
		 * Validates a list of mappings with the name of the list as single key, such as the onException of a route configuration.
		 */
		private void validateWrappedList(String name, Node value) {
			if (value instanceof SequenceNode) {
				for (Node item : ((SequenceNode) value).getValue()) {
					Node wrapped = getValue(item, name);
					if (wrapped != null && !isNull(wrapped)) {
						validateNode(model.getNode(name), wrapped);
					}
				}
			}
		}

		/**
		 * Reports the steps that the Camel version of the catalog does not know, and the deprecated ones. The catalog
		 * finds the names of its version only, the models of the other names are the ones of the bundled catalog.
		 */
		private void validateStepName(String name, Node keyNode, Child child) {
			boolean known = model.isOwnModel() && child != null || catalog.findModelNames().contains(name);
			EipModel eipModel = known ? catalog.eipModel(name) : null;
			if (!known) {
				addDiagnostic(getRange(keyNode), String.format("Unknown Camel EIP or step: '%s'", name), DiagnosticSeverity.Warning, null);
			} else if (eipModel != null && eipModel.isDeprecated()) {
				addDiagnostic(getRange(keyNode), String.format("'%s' is deprecated", name), DiagnosticSeverity.Hint, null);
			}
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

		private void addDiagnostic(Range range, String message, DiagnosticSeverity severity, String code) {
			diagnostics.add(new Diagnostic(range, message, severity, APACHE_CAMEL_VALIDATION, code));
		}
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
