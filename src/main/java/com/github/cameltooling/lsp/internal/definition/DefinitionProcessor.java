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
package com.github.cameltooling.lsp.internal.definition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.parser.helper.CamelXmlHelper;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.PathParamURIInstance;
import com.github.cameltooling.lsp.internal.parser.ParserXMLFileHelper;

public class DefinitionProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DefinitionProcessor.class);
	
	private ParserXMLFileHelper parserXMLFileHelper = new ParserXMLFileHelper();
	private TextDocumentItem textDocumentItem;

	public DefinitionProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}
	
	@SuppressWarnings("squid:S1452")
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> getDefinitions(Position position) {
		if (textDocumentItem.getUri().endsWith(".xml")) {
			String camelComponentUri = parserXMLFileHelper.getCamelComponentUri(textDocumentItem, position);
			if (camelComponentUri != null) {
				CamelURIInstance camelURIInstance = parserXMLFileHelper.createCamelURIInstance(textDocumentItem, position, camelComponentUri);
				if (camelURIInstance != null && "ref".equals(camelURIInstance.getComponentName())) {
					Set<PathParamURIInstance> pathParams = camelURIInstance.getComponentAndPathUriElementInstance().getPathParams();
					if(!pathParams.isEmpty()) {
						String refId = pathParams.iterator().next().getValue();
						return searchEndpointsWithId(refId);
					}
				}
			}
		}
		return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
	}

	private CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> searchEndpointsWithId(String refId) {
		try {
			List<Node> allEndpoints = parserXMLFileHelper.getAllEndpoints(textDocumentItem);
			for (Node endpoint : allEndpoints) {
				String id = CamelXmlHelper.getSafeAttribute(endpoint, "id");
				if (refId.equals(id)) {
					return CompletableFuture.completedFuture(Either.forLeft(Arrays.asList(parserXMLFileHelper.retrieveLocation(endpoint, textDocumentItem))));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Cannot compute defintions for "+ textDocumentItem.getUri(), e);
		}
		return CompletableFuture.completedFuture(Either.forLeft(Collections.emptyList()));
	}

}
