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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;

import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyEntryInstance;

public class CamelKModelinePropertyOption implements ICamelKModelineOptionValue {

	private CamelPropertyEntryInstance value;
	private int startPosition;
	private String fullStringValue;

	public CamelKModelinePropertyOption(String value, int startPosition) {
		this.value = new CamelPropertyEntryInstance(value, new Position(0, startPosition), null);
		this.fullStringValue = value;
		this.startPosition = startPosition;
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return value.getEndPositionInLine();
	}

	@Override
	public String getValueAsString() {
		return fullStringValue;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(int positionInLine, CompletableFuture<CamelCatalog> camelCatalog) {
		return value.getCompletions(new Position(0, positionInLine), camelCatalog);
	}

}
