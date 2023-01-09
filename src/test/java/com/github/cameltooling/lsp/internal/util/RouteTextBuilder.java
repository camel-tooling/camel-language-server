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

import org.eclipse.lsp4j.Position;

public class RouteTextBuilder {
	
	public static final String XML_PREFIX_FROM = "<from uri=\"";
	private static final String XML_SUFFIX_FROM_SPRING = "\" xmlns=\"http://camel.apache.org/schema/spring\"/>\n";
	private static final String XML_SUFFIX_FROM_BLUEPRINT = "\" xmlns=\"http://camel.apache.org/schema/blueprint\"/>\n";

	private static final String CAMEL_ROUTEBUILDER_IMPORT
			= "import org.apache.camel.builder.RouteBuilder;";

	private static final String CLASS_DECLARATION = "public class TestRoute";

	private static final String CAMEL_ROUTEBUILDER_EXTEMD = "extends RouteBuilder";

	private static final String CONFIGURE_METHOD_DECLARATION = "public void configure()";

	
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
	 * @param javaClassContent
	 * @return builds an empty Java class with the specified content and the cursor position inside and after
	 *          contents
	 */
	public static BlueprintContentWithPosition createJavaBlueprintClass(String javaClassContent) {
		String newLine = System.getProperty("line.separator");
		String[] contentSplit = javaClassContent.split(newLine);
		int lineOffset = contentSplit.length;
		int characterOffset = contentSplit[contentSplit.length-1].length();

		if (javaClassContent.startsWith(newLine)) {
			lineOffset += 1;
		}

		if (javaClassContent.endsWith(newLine)) {
			lineOffset +=1;
			characterOffset = 0;
		}

		return new BlueprintContentWithPosition(
				CLASS_DECLARATION + newLine +
				"{" + newLine
				+ javaClassContent + newLine
				+ "}" + newLine
				, 2+lineOffset, characterOffset);
	}

	/**
	 * @param camelRoute
	 * @return builds an empty Java class with the specified content and the cursor position placed inside configure
	 *          method and after content.
	 */
	public static BlueprintContentWithPosition createJavaBlueprintCamelRoute(String camelRoute) {
		String newLine = System.getProperty("line.separator");
		String[] contentSplit = camelRoute.split(newLine);
		int lineOffset = contentSplit.length - 1;
		int characterOffset = contentSplit[contentSplit.length-1].length();

		if (camelRoute.startsWith(newLine)) {
			lineOffset += 1;
		}

		if (camelRoute.endsWith(newLine)) {
			lineOffset +=1;
			characterOffset = 0;
		}

		return new BlueprintContentWithPosition(
				CAMEL_ROUTEBUILDER_IMPORT + newLine +
						CLASS_DECLARATION + newLine +
						CAMEL_ROUTEBUILDER_EXTEMD + newLine +
						"{" + newLine +
						CONFIGURE_METHOD_DECLARATION + "{" + newLine +
						camelRoute + newLine +
						"}" + newLine +
						"}" + newLine
				, 5 + lineOffset,characterOffset);
	}

	public static class BlueprintContentWithPosition {
		public String content;
		public Position position;

		public BlueprintContentWithPosition(String content, Position position) {
			this.content = content;
			this.position = position;
		}

		public BlueprintContentWithPosition(String content, int line, int character) {
			this.content = content;
			this.position = new Position(line, character);
		}
	}
}
