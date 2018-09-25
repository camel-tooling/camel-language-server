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
package com.github.cameltooling.lsp.internal.instancemodel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.model.ComponentModel;

public abstract class CamelUriElementInstance {
	
	private int startPositionInUri;
	private int endPositionInUri;
	private TextDocumentItem document;

	public CamelUriElementInstance(int startPositionInUri, int endPositionInUri) {
		this.startPositionInUri = startPositionInUri;
		this.endPositionInUri = endPositionInUri;
	}

	public int getStartPositionInUri() {
		return startPositionInUri;
	}

	public int getEndPositionInUri() {
		return endPositionInUri;
	}

	public boolean isInRange(int position) {
		return startPositionInUri <= position && position <= endPositionInUri;
	}
	
	public TextDocumentItem getDocument() {
		return document;
	}

	public void setDocument(TextDocumentItem document) {
		this.document = document;
	}

	public int getStartPositionInLine() {
		return getCamelUriInstance().getStartPositionInDocument().getCharacter() + getStartPositionInUri();
	}
	
	public int getEndPositionInLine() {
		return getCamelUriInstance().getStartPositionInDocument().getCharacter() + getEndPositionInUri();
	}
	
	public abstract CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri, TextDocumentItem docItem);
	
	public abstract String getComponentName();
	
	public abstract String getDescription(ComponentModel componentModel);
	
	public abstract CamelURIInstance getCamelUriInstance();
}
