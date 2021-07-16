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

/**
 * Platform information about OS and JVM.
 * 
 * Mostly duplicated from Eclipse Lemminx, the xml language server
 */
public class Platform {

	private static final String UNKNOWN_VALUE = "unknown";

	private static final OS os = new OS();
	private static final JVM jvm = new JVM();

	private Platform() {
	}

	/**
	 * Returns the OS information
	 *
	 * @return the OS information
	 */
	public static OS getOS() {
		return os;
	}

	/**
	 * Returns the JVM information
	 *
	 * @return the JVM information
	 */
	public static JVM getJVM() {
		return jvm;
	}

	/**
	 * Returns the system property from the given key and "unknown" otherwise.
	 *
	 * @param key the property system key
	 * @return the system property from the given key and "unknown" otherwise.
	 */
	static String getSystemProperty(String key) {
		try {
			String property = System.getProperty(key);
			return property == null || property.isEmpty() ? UNKNOWN_VALUE : property;
		} catch (SecurityException e) {
			return UNKNOWN_VALUE;
		}
	}

	/**
	 * Returns the server details, using the format:
	 *
	 * <pre>
	 * Camel Language Server info:
	 *  - Java : (path to java.home])
	 * </pre>
	 *
	 * @return the formatted server details
	 */
	public static String details() {
		StringBuilder details = new StringBuilder();
		append(details, "Java", jvm.getJavaHome());
		append(details, "VM Version", jvm.getVersion());
		return details.toString();
	}

	private static void append(StringBuilder sb, String key, String value) {
		sb.append(System.lineSeparator()).append(" - ").append(key);
		if (value != null) {
			sb.append(" : ").append(value);
		}
	}
}
