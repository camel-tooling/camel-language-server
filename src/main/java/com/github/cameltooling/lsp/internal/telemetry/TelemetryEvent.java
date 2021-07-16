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

/**
 * Telemetry event
 * 
 * Mostly duplicated from Eclipse Lemminx, the xml language server
 */
public class TelemetryEvent {

	public final String name;
	public final Map<String, Object> properties;

	TelemetryEvent() {
		this("", null);
	}

	TelemetryEvent(String name, Map<String, Object> properties) {
		this.name = name;
		this.properties = properties;
	}
}
