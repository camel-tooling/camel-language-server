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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;

public class CamelKModelineResourceOption implements ICamelKModelineOptionValue {
	
	private static final String PREFIX_FILE = "file:";

	private String value;
	private int startPosition;
	private int startLine;
	private int endLine;
	private CamelKModelineResourceFileOption resourceFileValue;

	public CamelKModelineResourceOption(String value, int startPosition, String uri, int startLine, int endLine) {
		this.value = value;
		this.startPosition = startPosition;
		this.startLine = startLine;
		this.endLine = endLine;
		if(value.startsWith(PREFIX_FILE)) {
			int endPosition = value.indexOf('@');
			if(endPosition != -1) {
				this.resourceFileValue = new CamelKModelineResourceFileOption(value.substring(PREFIX_FILE.length(), endPosition), startPosition + PREFIX_FILE.length(), uri, startLine, endLine);
			} else {
				this.resourceFileValue = new CamelKModelineResourceFileOption(value.substring(PREFIX_FILE.length()), startPosition + PREFIX_FILE.length(), uri, startLine, endLine);
			}
		}
	}

	@Override
	public int getStartLine() {
		return startLine;
	}

	@Override
	public int getEndLine() {
		return endLine;
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return startPosition + value.length();
	}

	@Override
	public String getValueAsString() {
		return value;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(position == startPosition) {
			CompletionItem configmap = new CompletionItem("configmap:");
			configmap.setDocumentation("Add a runtime resource from a Configmap (syntax: configmap:name[/key][@path], "
					+ "where name represents the configmap name, "
					+ "key optionally represents the configmap key to be filtered and path represents the destination path)");
			CompletionItem secret = new CompletionItem("secret:");
			secret.setDocumentation("Add a runtime resource from a Secret (syntax: secret:name[/key][@path], "
					+ "where name represents the secret name, "
					+ "key optionally represents the secret key to be filtered and path represents the destination path)");
			CompletionItem file = new CompletionItem(PREFIX_FILE);
			file.setDocumentation("Add a runtime resource from a file (syntax: file:name[@path], "
					+ "where name represents the local file path and path represents the destination path)");
			return CompletableFuture.completedFuture(Arrays.asList(configmap, secret, file));
		} else if(resourceFileValue != null && resourceFileValue.isInRange(position)) {
			return resourceFileValue.getCompletions(position, camelCatalog);
		}
		return ICamelKModelineOptionValue.super.getCompletions(position, camelCatalog);
	}

}
