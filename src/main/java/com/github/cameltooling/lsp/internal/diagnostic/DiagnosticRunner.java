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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.ConfigurationPropertiesValidationResult;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.parser.model.CamelEndpointDetails;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.CamelLanguageServer;

/**
 * @author lheinema
 */
public class DiagnosticRunner {

	private CamelLanguageServer camelLanguageServer;
	private EndpointDiagnosticService endpointDiagnosticService;
	private ConfigurationPropertiesDiagnosticService configurationPropertiesDiagnosticService;
	private CamelKModelineDiagnosticService camelKModelineDiagnosticService;
	private ConnectedModeDiagnosticService connectedModeDiagnosticService;
	private Map<String, CompletableFuture<Void>> lastTriggeredDiagnostic = new HashMap<>();

	public DiagnosticRunner(CompletableFuture<CamelCatalog> camelCatalog, CamelLanguageServer camelLanguageServer) {
		this.camelLanguageServer = camelLanguageServer;
		endpointDiagnosticService = new EndpointDiagnosticService(camelCatalog);
		configurationPropertiesDiagnosticService = new ConfigurationPropertiesDiagnosticService(camelCatalog);
		camelKModelineDiagnosticService = new CamelKModelineDiagnosticService();
		connectedModeDiagnosticService = new ConnectedModeDiagnosticService();
	}

	public void compute(DidSaveTextDocumentParams params) {
		String camelText = retrieveFullText(params);
		computeDiagnostics(camelText, camelLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()));
	}

	public void compute(DidChangeTextDocumentParams params) {
		String camelText = params.getContentChanges().get(0).getText();
		
		computeDiagnostics(camelText, camelLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()));
	}

	public void compute(DidOpenTextDocumentParams params) {
		String camelText = params.getTextDocument().getText();
		computeDiagnostics(camelText, params.getTextDocument());
	}

	public void computeDiagnostics(String camelText, TextDocumentItem documentItem) {
		String uri = documentItem.getUri();
		CompletableFuture<Void> previousComputation = lastTriggeredDiagnostic.get(uri);
		if (previousComputation != null) {
			previousComputation.cancel(true);
		}
		CompletableFuture<Void> lastTriggeredComputation = CompletableFuture.runAsync(() -> {
			Map<CamelEndpointDetails, EndpointValidationResult> endpointErrors = endpointDiagnosticService.computeCamelEndpointErrors(camelText, uri);
			TextDocumentItem openedDocument = camelLanguageServer.getTextDocumentService().getOpenedDocument(uri);
			List<Diagnostic> diagnostics = endpointDiagnosticService.converToLSPDiagnostics(camelText, endpointErrors, openedDocument);
			Map<String, ConfigurationPropertiesValidationResult> configurationPropertiesErrors = configurationPropertiesDiagnosticService.computeCamelConfigurationPropertiesErrors(camelText, uri);
			diagnostics.addAll(configurationPropertiesDiagnosticService.converToLSPDiagnostics(configurationPropertiesErrors));
			diagnostics.addAll(camelKModelineDiagnosticService.compute(camelText, documentItem));
			diagnostics.addAll(connectedModeDiagnosticService.compute(camelText, documentItem));
			camelLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
			lastTriggeredDiagnostic.remove(uri);
		});
		lastTriggeredDiagnostic.put(uri, lastTriggeredComputation);
	}

	private String retrieveFullText(DidSaveTextDocumentParams params) {
		String camelText = params.getText();
		if (camelText == null) {
			camelText = camelLanguageServer.getTextDocumentService().getOpenedDocument(params.getTextDocument().getUri()).getText();
		}
		return camelText;
	}

	public void clear(String uri) {
		CompletableFuture<Void> previousComputation = lastTriggeredDiagnostic.get(uri);
		if (previousComputation != null) {
			try {
				previousComputation.cancel(true);
			} catch (CancellationException ce) {
				// Do nothing
			}
		}
		camelLanguageServer.getClient().publishDiagnostics(new PublishDiagnosticsParams(uri, Collections.emptyList()));
	}
}
