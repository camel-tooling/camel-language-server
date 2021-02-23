/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.catalog.util;

import org.apache.camel.util.StringHelper;

/**
 * Various utility methods.
 */
public final class StringUtils {

	private StringUtils() {
	}

	/**
	 * Gets the value as a Camel component name
	 * 
	 * @param val the value
	 * @return the component name
	 */
	public static String asComponentName(String val) {
		if (val == null) {
			return null;
		}

		int pos = val.indexOf(':');
		if (pos > 0) {
			return val.substring(0, pos);
		}
		return null;
	}

	/**
	 * Is the string empty
	 * 
	 * @param str the string
	 * @return true if empty
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.trim().length() == 0;
	}
	
	/**
	 * Workaround to https://issues.apache.org/jira/browse/CAMEL-16250
	 * 
	 * @param dashedName
	 * @return the camel case named, keeping last dash at end of the name.
	 * 			It allows to detect when an invalid value is provided with just the dash that it is too much
	 */
	public static String dashToCamelCase(String dashedName) {
		if(dashedName.endsWith("-")) {
			return StringHelper.dashToCamelCase(dashedName.substring(0, dashedName.length() - 1)) + "-";
		} else {
			return StringHelper.dashToCamelCase(dashedName);
		}
	}

}
