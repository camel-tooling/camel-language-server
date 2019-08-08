/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public class CamelApplicationPropertiesCompletionProcessor {

	private static final String CAMEL_KEY_PREFIX = "camel.";
	private TextDocumentItem textDocumentItem;

	public CamelApplicationPropertiesCompletionProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		if (textDocumentItem != null && CAMEL_KEY_PREFIX.length() == position.getCharacter()) {
			String line = new ParserFileHelperUtil().getLine(textDocumentItem, position);
			if (line.startsWith(CAMEL_KEY_PREFIX)) {
				return getTopLevelCamelCompletion();
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	protected CompletableFuture<List<CompletionItem>> getTopLevelCamelCompletion() {
		List<CompletionItem> completions = new ArrayList<>();
		completions.add(new CompletionItem("component"));
		completions.add(new CompletionItem("main"));
		completions.add(new CompletionItem("rest"));
		completions.add(new CompletionItem("hystrix"));
		return CompletableFuture.completedFuture(completions);
	}

}
