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
package com.github.cameltooling.lsp.internal.hover;

import java.util.Collections;
import java.util.function.Function;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.util.StringHelper;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;
import com.github.cameltooling.lsp.internal.instancemodel.CamelUriElementInstance;

public class CamelURIHoverFuture implements Function<CamelCatalog, Hover> {
	
	private CamelUriElementInstance uriElement;
	
	public CamelURIHoverFuture(CamelUriElementInstance uriElement) {
		this.uriElement = uriElement;
	}

	@Override
	public Hover apply(CamelCatalog camelCatalog) {
		String componentUsedName = uriElement.getComponentName();
		String componentJSonSchema = camelCatalog.componentJSonSchema(componentUsedName);
		if (componentJSonSchema == null && componentUsedName.contains("-")) {
			componentJSonSchema = camelCatalog.componentJSonSchema(StringHelper.dashToCamelCase(componentUsedName));
		}
		if (componentJSonSchema != null) {
			Hover hover = new Hover();
			ComponentModel componentModel = ModelHelper.generateComponentModel(componentJSonSchema, true);
			hover.setContents(Collections.singletonList((Either.forLeft(uriElement.getDescription(componentModel)))));
			return hover;
		}
		return null;
	}

}
