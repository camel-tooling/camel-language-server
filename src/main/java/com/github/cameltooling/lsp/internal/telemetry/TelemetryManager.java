/*******************************************************************************
* Copyright (c) 2021 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package com.github.cameltooling.lsp.internal.telemetry;

import java.util.Map;

import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Telemetry manager.
 * 
 * Mostly duplicated from Eclipse Lemminx, the xml language server
 *
 * @author Angelo ZERR
 */
public class TelemetryManager {

	/**
	 * "startup" telemetry event name
	 */
	public static final String STARTUP_EVENT_NAME = "camel.lsp.server.initialized";

	private final LanguageClient languageClient;

	public TelemetryManager(LanguageClient languageClient) {
		this.languageClient = languageClient;
	}

	/**
	 * Send a telemetry event on start of the XML server
	 *
	 */
	public void onInitialized() {
		telemetryEvent(STARTUP_EVENT_NAME, InitializationTelemetryInfo.getInitializationTelemetryInfo());
	}

	/**
	 * The telemetry notification is sent from the server to the client to ask the
	 * client to log a telemetry event.
	 */
	private void telemetryEvent(String eventName, Map<String, Object> object) {
		languageClient.telemetryEvent(new TelemetryEvent(eventName, object));
	}

}
