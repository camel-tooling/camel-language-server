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
package com.github.cameltooling.lsp.internal.modelinemodel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.completion.CompletionResolverUtils;

public class CamelKModelineResourceOption implements ICamelKModelineOptionValue {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelKModelineResourceOption.class);
	private String value;
	private int startPosition;
	private String documentItemUri;

	public CamelKModelineResourceOption(String value, int startPosition, String documentItemUri) {
		this.value = value;
		this.startPosition = startPosition;
		this.documentItemUri = documentItemUri;
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + value.length();
	}

	@Override
	public String getValueAsString() {
		return value;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int position, CompletableFuture<CamelCatalog> camelCatalog) {
		try {
			Path documentUriPath = Paths.get(new URI(documentItemUri));
			if(documentUriPath != null && documentUriPath.toFile().exists()) {
				Path documentUriParentPath = documentUriPath.getParent();
				if(documentUriParentPath != null && documentUriParentPath.toFile().exists()) {
					return CompletableFuture.completedFuture(retrieveCompletionItemsForPotentialFiles(documentUriPath, documentUriParentPath));
				}
			}
		} catch (URISyntaxException | IllegalArgumentException | IOException exception) {
			LOGGER.debug("Cannot provide completion for resource parameter", exception);
		}
		return ICamelKModelineOptionValue.super.getCompletions(position, camelCatalog);
	}

	private List<CompletionItem> retrieveCompletionItemsForPotentialFiles(Path documentUriPath, Path documentUriParentPath) throws IOException {
		try (Stream<Path> pathStream = Files.walk(documentUriParentPath)){
			return pathStream
					.filter(Files::isRegularFile)
					.filter(path -> !path.equals(documentUriPath))
					.map(documentUriParentPath::relativize)
					.map(Path::toString)
					.map(CompletionItem::new)
					.map(completionItem -> {
						CompletionResolverUtils.applyTextEditToCompletionItem(this, completionItem);
						return completionItem;
					})
					.collect(Collectors.toList());
		}
	}
}
