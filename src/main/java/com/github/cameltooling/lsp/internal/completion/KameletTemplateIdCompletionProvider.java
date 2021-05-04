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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.kamelets.catalog.KameletsCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.instancemodel.PathParamURIInstance;

public class KameletTemplateIdCompletionProvider {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(KameletTemplateIdCompletionProvider.class);

	public CompletableFuture<List<CompletionItem>> get(PathParamURIInstance pathParamURIInstance) {
		KameletsCatalog kameletsCatalog;
		try {
			kameletsCatalog = new KameletsCatalog();
			List<String> kameletsName = kameletsCatalog.getKameletsName();
			List<CompletionItem> completionItems = kameletsName
					.stream()
					.map(name -> {
						CompletionItem completionItem = new CompletionItem(name);
						CompletionResolverUtils.applyTextEditToCompletionItem(pathParamURIInstance, completionItem);
						return completionItem;
					})
					.collect(Collectors.toList());
			return CompletableFuture.completedFuture(completionItems);
		} catch (IOException e) {
			LOGGER.warn("Cannot determine completion for Kamelet template Ids", e);
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
		
	}

}
