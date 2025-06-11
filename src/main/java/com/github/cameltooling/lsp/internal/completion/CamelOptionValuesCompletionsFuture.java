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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.CompletionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.model.EndpointOptionModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.instancemodel.OptionParamValueURIInstance;
import com.github.cameltooling.lsp.internal.kubernetes.KubernetesConfigManager;

import io.fabric8.kubernetes.client.KubernetesClient;

public class CamelOptionValuesCompletionsFuture implements Function<CamelCatalog, List<CompletionItem>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CamelOptionValuesCompletionsFuture.class);

	private static final String BOOLEAN_TYPE = "boolean";
	private OptionParamValueURIInstance optionParamValueURIInstance;
	private String filterString;

	public CamelOptionValuesCompletionsFuture(OptionParamValueURIInstance optionParamValueURIInstance, String filterText) {
		this.optionParamValueURIInstance = optionParamValueURIInstance;
		this.filterString = filterText;
	}

	@Override
	public List<CompletionItem> apply(CamelCatalog camelCatalog) {
		Optional<EndpointOptionModel> endpointModel = retrieveEndpointOptionModel(camelCatalog);
		if (endpointModel.isPresent()) {
			EndpointOptionModel endpointOptionModel = endpointModel.get();
			List<String> enums = endpointOptionModel.getEnums();
			if (enums != null && !enums.isEmpty()) {
				return computeCompletionForEnums(enums);
			} else if (BOOLEAN_TYPE.equals(endpointOptionModel.getType())) {
				CompletionItem trueItem = new CompletionItem(Boolean.TRUE.toString());
				CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, trueItem);
				CompletionItem falseItem = new CompletionItem(Boolean.FALSE.toString());
				CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, falseItem);
				Stream<CompletionItem> values = Stream.of(trueItem, falseItem);
				return values.filter(FilterPredicateUtils.matchesCompletionFilter(filterString)).collect(Collectors.toList());
			} else if (optionParamValueURIInstance.getComponentName().startsWith("kubernetes-")
					&& "namespace".equals(optionParamValueURIInstance.getOptionParamURIInstance().getKey().getKeyName())) {
				try (KubernetesClient client = KubernetesConfigManager.getInstance().getClient()) {
					return client.namespaces().list().getItems().stream().map(namespace -> {
						var completionItem = new CompletionItem(namespace.getMetadata().getName());
						CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, completionItem);
						return completionItem;
					}).collect(Collectors.toList());
				} catch (Exception e) {
					LOGGER.error("Error while trying to provide completion for Kubernetes connected mode", e);
				}
			} else if ("lang".equalsIgnoreCase(endpointOptionModel.getName())
					&& optionParamValueURIInstance.getComponentName().startsWith("twitter-")) {
				List<CompletionItem> items = new ArrayList<>();
				//We will just rely on Java to be updated for this
				for (String lang : Locale.getISOLanguages()) {
					CompletionItem langItem = new CompletionItem(lang);
					CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, langItem);
					items.add(langItem);
				}
				return items;
			}

			//Check based on the value of the parameter
			//if we get here, all the previous checks are done,
			// so we can return it directly
			return getCompletionForKubernetes();
		}
		return Collections.emptyList();
	}

	private List<CompletionItem> getCompletionForKubernetes() {
		final var interestingPosition =
				optionParamValueURIInstance.getValueName().length() > filterString.length() ?
						filterString.length() + 1 : filterString.length();
		final var value = optionParamValueURIInstance.getValueName().substring(0, interestingPosition);
		var kubernetesPlaceholders = new ArrayList<CompletionItem>();

		// Make sure we have the cursor after a {{
		if (StringUtils.contains(value, "{{") &&
				//Either it is not closed by }}
				(!StringUtils.contains(value, "}}") ||
						// or }} is after the cursor
						(value.lastIndexOf("}}") < value.lastIndexOf("{{")))) {

			//Get the string before the placeholder (starting with {{)
			final var pre = value.substring(0, value.lastIndexOf("{{"));

			// Now we get the string after the placeholder.
			// This is tricky because we don't even know if the placeholder is already closed.
			// interestingPosition is where the cursor is supposed to be
			var lastPart = optionParamValueURIInstance.getValueName().substring(interestingPosition);

			// The cursor is between {{ and }} because we found a }} after the cursor
			if (lastPart.indexOf("}}") >= 0 &&
					// This }} detected is closing the placeholder we are guessing
					(lastPart.indexOf("}}") < lastPart.indexOf("{{")
							//Or it is the only placeholder closing here, so it must be ours
							|| lastPart.indexOf("{{") < 0)) {
				//Let's get whatever is behind the detected }}
				lastPart = lastPart.substring(lastPart.indexOf("}}") + 2);
			}

			final var post = lastPart;
			try (KubernetesClient client = KubernetesConfigManager.getInstance().getClient()) {
				if (client instanceof NamespacedKubernetesClient nsClient) {
					kubernetesPlaceholders.addAll(nsClient.inAnyNamespace().secrets().list().getItems().stream().flatMap(element ->
							element.getData().keySet().stream().map(k -> {
								CompletionItem item = new CompletionItem(
										"{{secret:" + element.getMetadata().getName() + "/" + k + "}}");
								item.setInsertText(pre + item.getLabel() + post);
								item.setFilterText(pre + item.getLabel());
								CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, item);
								return item;
							})).collect(Collectors.toList()));
					kubernetesPlaceholders.addAll(nsClient.inAnyNamespace().configMaps().list().getItems().stream().flatMap(element ->
							element.getData().keySet().stream().map(k -> {
								CompletionItem item = new CompletionItem(
										"{{configmap:" + element.getMetadata().getName() + "/" + k + "}}");
								item.setInsertText(pre + item.getLabel() + post);
								item.setFilterText(pre + item.getLabel());
								CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, item);
								return item;
							})).collect(Collectors.toList()));
				}
			} catch (Exception e) {
				LOGGER.error("Error while trying to provide completion for Kubernetes connected mode", e);
			}
		}

		return kubernetesPlaceholders;
	}

	private List<CompletionItem> computeCompletionForEnums(List<String> enums) {
		List<CompletionItem> completionItems = new ArrayList<>();
		for (String enumValue : enums) {
			CompletionItem item = new CompletionItem(enumValue);
			CompletionResolverUtils.applyTextEditToCompletionItem(optionParamValueURIInstance, item);
			completionItems.add(item);
		}
		return completionItems.stream().filter(FilterPredicateUtils.matchesCompletionFilter(filterString)).collect(Collectors.toList());
	}

	private Optional<EndpointOptionModel> retrieveEndpointOptionModel(CamelCatalog camelCatalog) {
		String componentName = optionParamValueURIInstance.getOptionParamURIInstance().getComponentName();
		String keyName = optionParamValueURIInstance.getOptionParamURIInstance().getKey().getKeyName();
		List<EndpointOptionModel> endpointOptions = ModelHelper.generateComponentModel(camelCatalog.componentJSonSchema(componentName), true).getEndpointOptions();
		return endpointOptions.stream()
				.filter(endpoint -> keyName.equals(endpoint.getName()))
				.findAny();
	}
}
