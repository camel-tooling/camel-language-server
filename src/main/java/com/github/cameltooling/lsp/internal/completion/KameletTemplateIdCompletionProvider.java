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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.kamelets.catalog.KameletsCatalog;
import org.apache.camel.kamelets.catalog.model.KameletTypeEnum;
import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.catalog.util.KameletsCatalogManager;
import com.github.cameltooling.lsp.internal.instancemodel.PathParamURIInstance;

import org.apache.camel.v1.Kamelet;

public class KameletTemplateIdCompletionProvider {
	
	private KameletsCatalogManager kameletsCatalogManager;

	public KameletTemplateIdCompletionProvider(KameletsCatalogManager kameletsCatalogManager) {
		this.kameletsCatalogManager = kameletsCatalogManager;
	}

	public CompletableFuture<List<CompletionItem>> get(PathParamURIInstance pathParamURIInstance) {
		Collection<Kamelet> kamelets = retrievePotentialKamelets(pathParamURIInstance);
			
		List<CompletionItem> completionItems = kamelets
			.stream()
				.map(kamelet -> {
					CompletionItem completionItem = new CompletionItem(kamelet.getMetadata().getName());
					CompletionResolverUtils.applyTextEditToCompletionItem(pathParamURIInstance, completionItem);
					completionItem.setDocumentation(kamelet.getSpec().getDefinition().getDescription());
					return completionItem;
				})
				.collect(Collectors.toList());
		return CompletableFuture.completedFuture(completionItems);
	}

	private List<Kamelet> retrievePotentialKamelets(PathParamURIInstance pathParamURIInstance) {
		KameletsCatalog kameletsCatalog = kameletsCatalogManager.getCatalog();
		if(kameletsCatalog != null) {
			if (pathParamURIInstance.getCamelUriInstance().isProducer()) {
				return kameletsCatalog.getKameletsByType(KameletTypeEnum.SINK.type());
			} else {
				return kameletsCatalog.getKameletsByType(KameletTypeEnum.SOURCE.type());
			}
		} else {
			return Collections.emptyList();
		}
	}

}
