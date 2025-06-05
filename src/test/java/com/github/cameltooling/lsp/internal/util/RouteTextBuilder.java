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
package com.github.cameltooling.lsp.internal.util;

public class RouteTextBuilder {
	
	public static final String XML_PREFIX_FROM = "<from uri=\"";
	private static final String XML_SUFFIX_FROM_SPRING = "\" xmlns=\"http://camel.apache.org/schema/spring\"/>\n";
	private static final String XML_SUFFIX_FROM_BLUEPRINT = "\" xmlns=\"http://camel.apache.org/schema/blueprint\"/>\n";
	private static final String XML_SUFFIX_FROM_XMLIO = "\" xmlns=\"http://camel.apache.org/schema/xml-io\"/>\n";
	
	/**
	 * @param camelUri
	 * @return a single line xml route with a from having camelUri as uri and with Spring namespace
	 */
	public static String createXMLSpringRoute(String camelUri) {
		return XML_PREFIX_FROM + camelUri + XML_SUFFIX_FROM_SPRING;
	}
	
	/**
	 * @param camelUri
	 * @return a single line xml route with a from having camelUri as uri and with Blueprint namespace
	 */
	public static String createXMLBlueprintRoute(String camelUri) {
		return XML_PREFIX_FROM + camelUri + XML_SUFFIX_FROM_BLUEPRINT;
	}
	
	/**
	 * @param camelUri
	 * @return a single line xml route with a from having camelUri as uri and with XML IO namespace
	 */
	public static String createXMLIORoute(String camelUri) {
		return XML_PREFIX_FROM + camelUri + XML_SUFFIX_FROM_XMLIO;
	}

}
