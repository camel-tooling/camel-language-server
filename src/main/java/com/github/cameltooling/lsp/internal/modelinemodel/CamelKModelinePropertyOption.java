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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyEntryInstance;

/**
 * The property option can be written with 2 patterns: property=aKey=aValue or property=file:/path/to/file.properties
 * 
 * The {@link CamelKModelinePropertyOption#singlePropertyValue} is used to represent aKey=aValue.
 * The {@link CamelKModelinePropertyOption#fileValue} is used to represent /path/to/file.properties.
 *
 */
public class CamelKModelinePropertyOption implements ICamelKModelineOptionValue {

	private static final String FILE_PREFIX = "file:";
	
	private CamelPropertyEntryInstance singlePropertyValue;
	private CamelKModelinePropertyFileOption fileValue;
	private int startPosition;
	private String fullStringValue;
	private int line;

	public CamelKModelinePropertyOption(String value, int startPosition, TextDocumentItem documentItem, int line) {
		this.line = line;
		if(value.startsWith(FILE_PREFIX)) {
			this.fileValue = new CamelKModelinePropertyFileOption(value.substring(FILE_PREFIX.length()), startPosition + FILE_PREFIX.length(), documentItem.getUri(), line);
		} else {
			this.singlePropertyValue = new CamelPropertyEntryInstance(value, new Position(0, startPosition), documentItem);
		}
		this.fullStringValue = value;
		this.startPosition = startPosition;
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return startPosition + fullStringValue.length();
	}

	@Override
	public String getValueAsString() {
		return fullStringValue;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int positionInLine, CompletableFuture<CamelCatalog> camelCatalog) {
		if(fileValue != null && fileValue.isInRange(positionInLine)) {
			return fileValue.getCompletions(positionInLine, camelCatalog);
		} else if(singlePropertyValue != null) {
			CompletableFuture<List<CompletionItem>> camelComponentPropertyCompletionFuture = singlePropertyValue.getCompletions(new Position(0, positionInLine), camelCatalog, null, null);
			if(positionInLine == getStartPositionInLine()) {
				return mergeFutures(camelComponentPropertyCompletionFuture, createFilePrefixCompletion());
			}
			return camelComponentPropertyCompletionFuture;
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
	}

	private CompletableFuture<List<CompletionItem>> createFilePrefixCompletion() {
		CompletionItem filePrefixCompletionItem = new CompletionItem(FILE_PREFIX);
		filePrefixCompletionItem.setDocumentation("Provide a properties file path");
		return CompletableFuture.completedFuture(Collections.singletonList(filePrefixCompletionItem));
	}

	private CompletableFuture<List<CompletionItem>> mergeFutures(CompletableFuture<List<CompletionItem>> future1, CompletableFuture<List<CompletionItem>> future2) {
		List<CompletableFuture<List<CompletionItem>>> allFutures = Arrays.asList(future1, future2);
		return CompletableFuture.allOf(future1, future2)
				.thenApply(avoid -> 
					allFutures.stream().flatMap(f -> f.join().stream()).collect(Collectors.toList())
					);
	}
	
	@Override
	public CompletableFuture<Hover> getHover(int characterPosition, CompletableFuture<CamelCatalog> camelCatalog) {
		if(singlePropertyValue != null) {
			return singlePropertyValue.getHover(new Position(0, characterPosition), camelCatalog, null);
		} else {
			return CompletableFuture.completedFuture(null);
		}
	}
	
	public int getLine() {
		return line;
	}

}
