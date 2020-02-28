/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.github.cameltooling.lsp.internal.diagnostic;

import java.util.List;
import java.util.Map;

import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.parser.model.CamelEndpointDetails;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

import com.github.cameltooling.lsp.internal.CamelLanguageServer;

/**
 * @author lheinema
 */
public class DiagnosticRunner {
	
	private DiagnosticService diagnosticServer;
	private CamelLanguageServer camelLanguageServer;
	private String camelText;
	private String uri;
	
	public DiagnosticRunner(DiagnosticService diagnosticServer, CamelLanguageServer camelLanguageServer, String camelText, String uri) {
		this.diagnosticServer = diagnosticServer;
		this.camelLanguageServer = camelLanguageServer;
		this.camelText = camelText;
		this.uri = uri;
		calculate();
	}
	
	private void calculate() {
		Map<CamelEndpointDetails, EndpointValidationResult> endpointErrors = diagnosticServer.computeCamelEndpointErrors(camelText, uri);
		List<Diagnostic> diagnostics = diagnosticServer.converToLSPDiagnostics(camelText, endpointErrors, camelLanguageServer.getTextDocumentService().getOpenedDocument(uri));
		camelLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
	}
}
