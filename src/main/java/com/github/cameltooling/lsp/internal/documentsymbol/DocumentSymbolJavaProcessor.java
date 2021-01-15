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

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.parser.RouteBuilderParser;
import org.apache.camel.parser.model.CamelEndpointDetails;
import org.apache.camel.parser.model.CamelNodeDetails;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentSymbolJavaProcessor extends AbstractDocumentSymbolProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentSymbolJavaProcessor.class);

	public DocumentSymbolJavaProcessor(TextDocumentItem textDocumentItem) {
		super(textDocumentItem);
	}

	public List<Either<SymbolInformation, DocumentSymbol>> getSymbolInformations() {
		try {
			JavaType<?> parsedJavaFile = Roaster.parse(textDocumentItem.getText());
			if (parsedJavaFile instanceof JavaClassSource) {
				JavaClassSource clazz = (JavaClassSource) parsedJavaFile;
				String absolutePathOfCamelFile = new File(URI.create(textDocumentItem.getUri())).getAbsolutePath();
				List<CamelNodeDetails> camelNodes = RouteBuilderParser.parseRouteBuilderTree(clazz, "", absolutePathOfCamelFile, true);
				List<CamelEndpointDetails> endpoints = new ArrayList<>();
				RouteBuilderParser.parseRouteBuilderEndpoints(clazz, "", absolutePathOfCamelFile, endpoints);
				return createSymbolInformations(camelNodes, endpoints);
			}
		} catch (Exception ex) {
			LOGGER.warn("Error while computing Document symbols for "+ textDocumentItem.getUri(), ex);
		}
		return Collections.emptyList();
	}

}
