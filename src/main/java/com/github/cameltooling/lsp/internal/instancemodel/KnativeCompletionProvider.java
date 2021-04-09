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
package com.github.cameltooling.lsp.internal.instancemodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;
import com.github.cameltooling.lsp.internal.kubernetes.KnativeConfigManager;

public class KnativeCompletionProvider {

	private static final String TYPE_EVENT = "event";
	private static final String TYPE_ENDPOINT = "endpoint";
	private static final String TYPE_CHANNEL = "channel";

	public CompletableFuture<List<CompletionItem>> get(PathParamURIInstance pathParamURIInstance) {
		Optional<String> typeParam = pathParamURIInstance.getCamelComponentAndPathUriInstance().getPathParams().stream()
				.filter(pathParam -> pathParam.getPathParamIndex() == 0).map(PathParamURIInstance::getValue)
				.findFirst();
		if (typeParam.isPresent()) {
			var client = KnativeConfigManager.getInstance().getClient();
			String type = typeParam.get();
			if (TYPE_CHANNEL.equals(type)) {
				List<CompletionItem> allChannels = new ArrayList<>();
				allChannels.addAll(client.inMemoryChannels().list().getItems().stream().map(channel -> {
					var completionItem = new CompletionItem(channel.getMetadata().getName());
					CompletionResolverUtils.applyTextEditToCompletionItem(pathParamURIInstance, completionItem);
					return completionItem;
				}).collect(Collectors.toList()));
				allChannels.addAll(client.channels().list().getItems().stream().map(channel -> {
					var completionItem = new CompletionItem(channel.getMetadata().getName());
					CompletionResolverUtils.applyTextEditToCompletionItem(pathParamURIInstance, completionItem);
					return completionItem;
				}).collect(Collectors.toList()));
				return CompletableFuture.completedFuture(allChannels);
			} else if (TYPE_ENDPOINT.equals(type)) {
				return CompletableFuture.completedFuture(client.services().list().getItems().stream().map(service -> {
					var completionItem = new CompletionItem(service.getMetadata().getName());
					CompletionResolverUtils.applyTextEditToCompletionItem(pathParamURIInstance, completionItem);
					return completionItem;
				}).collect(Collectors.toList()));
			} else if (TYPE_EVENT.equals(type)) {
				return CompletableFuture
						.completedFuture(client.eventTypes().list().getItems().stream().map(eventType -> {
							var completionItem = new CompletionItem(eventType.getMetadata().getName());
							CompletionResolverUtils.applyTextEditToCompletionItem(pathParamURIInstance, completionItem);
							return completionItem;
						}).collect(Collectors.toList()));
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

}
