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
package com.github.cameltooling.lsp.internal.completion.modeline;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Provides with the available completions for inserting new camel-k modelines
 *
 * @author joshiraez
 */
public class CamelKModelineInsertionProcessor {

	private final TextDocumentItem textDocumentItem;

	public CamelKModelineInsertionProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions() {
		return CompletableFuture.completedFuture(
				getCompletionCorrespondingToDocument().map(
						List::of
				).orElse(List.of())
		);
	}

	private Optional<CompletionItem> getCompletionCorrespondingToDocument() {
		return CamelKModelineFileType.getFileTypeCorrespondingToUri(textDocumentItem.getUri())
				.map(CamelKModelineFileType::getCompletion);
	}
}
