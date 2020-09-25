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
package com.github.cameltooling.lsp.internal.documentsymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

public class DocumentSymbolProcessor {

	private TextDocumentItem textDocumentItem;

	public DocumentSymbolProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}

	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> getDocumentSymbols() {
		return CompletableFuture.supplyAsync(() -> {
			List<Either<SymbolInformation, DocumentSymbol>> symbolInformations = new ArrayList<>();
			if (textDocumentItem.getUri().endsWith(".xml")) {
				return new DocumentSymbolXMLProcessor(textDocumentItem).getSymbolInformations();
			} else if (textDocumentItem.getUri().endsWith(".java")) {
				return new DocumentSymbolJavaProcessor(textDocumentItem).getSymbolInformations();
			}
			return symbolInformations;
		});
	}
}
