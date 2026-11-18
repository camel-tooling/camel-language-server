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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.camel.kamelets.catalog.model.KameletTypeEnum;
import org.apache.camel.v1.Kamelet;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.instancemodel.PathParamURIInstance;

public class KameletTemplateIdCompletionProvider {
	
	private KameletsCatalogManager kameletsCatalogManager;

	public KameletTemplateIdCompletionProvider(KameletsCatalogManager kameletsCatalogManager) {
		this.kameletsCatalogManager = kameletsCatalogManager;
	}

	public CompletableFuture<List<CompletionItem>> get(PathParamURIInstance pathParamURIInstance) {
		boolean producer = pathParamURIInstance.getCamelUriInstance().isProducer();
		return CompletableFuture.completedFuture(createItems(producer, item -> CompletionResolverUtils.applyTextEditToCompletionItem(pathParamURIInstance, item)));
	}

	/**
	 * @param producer whether the Kamelet is used by a producer, such as a to, or by a consumer, such as a from
	 * @param range the range of the Kamelet name that the completion replaces
	 * @return the Kamelets that can be used there
	 */
	public List<CompletionItem> get(boolean producer, Range range) {
		return createItems(producer, item -> item.setTextEdit(Either.forLeft(new TextEdit(range, item.getLabel()))));
	}

	private List<CompletionItem> createItems(boolean producer, Consumer<CompletionItem> textEditSetter) {
		return retrievePotentialKamelets(producer).entrySet()
			.stream()
				.map(kamelet -> {
					// the name of the Kamelet is the one Camel resolves, from the name of its file
					CompletionItem completionItem = new CompletionItem(kamelet.getKey());
					textEditSetter.accept(completionItem);
					completionItem.setDocumentation(kamelet.getValue().getSpec().getDefinition().getDescription());
					return completionItem;
				})
				.collect(Collectors.toList());
	}

	/**
	 * @return the sink and action Kamelets for a producer, the source Kamelets for a consumer
	 */
	private Map<String, Kamelet> retrievePotentialKamelets(boolean producer) {
		if (producer) {
			Map<String, Kamelet> kamelets = new LinkedHashMap<>(kameletsCatalogManager.getKameletsByType(KameletTypeEnum.SINK.type()));
			kamelets.putAll(kameletsCatalogManager.getKameletsByType(KameletTypeEnum.ACTION.type()));
			return kamelets;
		} else {
			return kameletsCatalogManager.getKameletsByType(KameletTypeEnum.SOURCE.type());
		}
	}

}
