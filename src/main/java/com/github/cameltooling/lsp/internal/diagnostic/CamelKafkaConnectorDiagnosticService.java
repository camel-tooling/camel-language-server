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
package com.github.cameltooling.lsp.internal.diagnostic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyEntryInstance;
import com.github.cameltooling.lsp.internal.parser.CamelKafkaUtil;

public class CamelKafkaConnectorDiagnosticService extends DiagnosticService {

	protected CamelKafkaConnectorDiagnosticService(CamelKafkaConnectorCatalogManager camelKafkaConnectorCatalogManager) {
		super(null, camelKafkaConnectorCatalogManager);
	}

	public Collection<Diagnostic> compute(String camelText, TextDocumentItem documentItem) {
		String docUri = documentItem.getUri();
		if(docUri.endsWith(".properties")) {
			String connectorClass = new CamelKafkaUtil().findConnectorClass(documentItem);
			Optional<CamelKafkaConnectorModel> connectorModelOptional = camelKafkaConnectorCatalogManager.findConnectorModel(connectorClass);
			if (connectorModelOptional.isPresent()) {
				List<Diagnostic> lspDiagnostics = new ArrayList<>();
				BufferedReader reader = new BufferedReader(new StringReader(camelText));
				int lineNumber = 0;
				String line;
				try {
					while((line = reader.readLine())!= null) {
						CamelPropertyEntryInstance camelPropertyEntryInstance = new CamelPropertyEntryInstance(line, new Position(lineNumber, 0), documentItem);
						lspDiagnostics.addAll(camelPropertyEntryInstance.validate(camelKafkaConnectorCatalogManager));
						lineNumber++;
					}
					return lspDiagnostics;
				} catch (IOException e) {
					logExceptionValidatingDocument(docUri, e);
				}
			}
		}
		return Collections.emptyList();
	}

}
