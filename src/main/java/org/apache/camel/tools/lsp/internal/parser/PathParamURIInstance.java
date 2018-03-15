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
package org.apache.camel.tools.lsp.internal.parser;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.tools.lsp.internal.completion.CamelComponentSchemaCompletionsFuture;
import org.eclipse.lsp4j.CompletionItem;

public class PathParamURIInstance extends CamelUriElementInstance {

	private String value;

	public PathParamURIInstance(String value, int startPosition, int endPosition) {
		super(startPosition, endPosition);
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
	@Override
	public CompletableFuture<List<CompletionItem>> getCompletions(CompletableFuture<CamelCatalog> camelCatalog, int positionInCamelUri) {
		if(getStartPosition() <= positionInCamelUri && positionInCamelUri <= getEndPosition()) {
			return camelCatalog.thenApply(new CamelComponentSchemaCompletionsFuture());
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof PathParamURIInstance) {
			return value.equals(((PathParamURIInstance) obj).getValue())
					&& getStartPosition() == ((PathParamURIInstance) obj).getStartPosition()
					&& getEndPosition() == ((PathParamURIInstance) obj).getEndPosition();
		}
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value, getStartPosition(), getEndPosition());
	}
	
	@Override
	public String toString() {
		return "Value: "+value+" start position:"+getStartPosition()+ " end position:"+getEndPosition();
	}
	
}
