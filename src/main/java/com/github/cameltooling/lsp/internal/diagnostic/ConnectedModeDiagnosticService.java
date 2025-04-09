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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.parser.model.CamelEndpointDetails;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.instancemodel.ComponentNameConstants;

public class ConnectedModeDiagnosticService extends DiagnosticService {

	public ConnectedModeDiagnosticService() {
		super(null);
	}

	public Collection<? extends Diagnostic> compute(String camelText, TextDocumentItem documentItem) {
		Set<Diagnostic> diagnostics = new HashSet<>();
		List<CamelEndpointDetails> endpoints = retrieveEndpoints(documentItem.getUri(), camelText);
		for (CamelEndpointDetails camelEndpointDetails : endpoints) {
			String endpointUri = camelEndpointDetails.getEndpointUri();
			if (endpointUri.startsWith(ComponentNameConstants.COMPONENT_NAME_KNATIVE)) {
				diagnostics.add(new Diagnostic(
						computeRange(camelText, documentItem, camelEndpointDetails),
						"If a connection to Kubernetes with Knative installed is active, the completion will be dynamically augmented.",
						DiagnosticSeverity.Hint,
						APACHE_CAMEL_VALIDATION,
						null));
			} else if(endpointUri.startsWith("kubernetes-")) {
				diagnostics.add(new Diagnostic(
						computeRange(camelText, documentItem, camelEndpointDetails),
						"If a connection to Kubernetes is active, the completion will be dynamically augmented.",
						DiagnosticSeverity.Hint,
						APACHE_CAMEL_VALIDATION,
						null));
			} else if (endpointUri.startsWith(ComponentNameConstants.COMPONENT_NAME_KAFKA)) {
				diagnostics.add(new Diagnostic(
						computeRange(camelText, documentItem, camelEndpointDetails),
						"If a local kafka instance is running, the completion will be dynamically augmented.",
						DiagnosticSeverity.Hint,
						APACHE_CAMEL_VALIDATION,
						null));
			}
		}
		return diagnostics;
	}	

}
