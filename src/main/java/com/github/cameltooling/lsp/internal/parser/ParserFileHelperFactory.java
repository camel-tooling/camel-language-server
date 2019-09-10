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
package com.github.cameltooling.lsp.internal.parser;

import org.eclipse.lsp4j.TextDocumentItem;

public class ParserFileHelperFactory {
	
	private static final String CAMELK_GROOVY_FILENAME_SUFFIX = ".camelk.groovy";
	private static final String CAMELK_KOTLIN_FILENAME_SUFFIX = ".camelk.kts";
	private static final String CAMELK_YAML_FILENAME_SUFFIX = ".camelk.yaml";
	private static final String CAMELK_JS_FILENAME_SUFFIX = ".camelk.js";
	private static final String SHEBANG_CAMEL_K = "#!/usr/bin/env camel-k";
	private static final String MODELINE_LIKE_CAMEL_K = "// camel-k:";

	public ParserFileHelper getCorrespondingParserFileHelper(TextDocumentItem textDocumentItem, int line) {
		ParserXMLFileHelper xmlParser = new ParserXMLFileHelper();
		String uri = textDocumentItem.getUri();
		if (uri.endsWith(".xml") && xmlParser.getCorrespondingCamelNodeForCompletion(textDocumentItem, line) != null) {
			return xmlParser;
		} else if(isCamelJavaDSL(textDocumentItem, uri)) {
			ParserJavaFileHelper javaParser = new ParserJavaFileHelper();
			if (javaParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
				return javaParser;
			}
		} else if(isCamelKGroovyDSL(textDocumentItem, uri)) {
			CamelKGroovyDSLParser camelKGroovyDSLParser = new CamelKGroovyDSLParser();
			if (camelKGroovyDSLParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
				return camelKGroovyDSLParser;
			}
		} else if(isCamelKYamlDSL(textDocumentItem, uri)) {
			CamelKYamlDSLParser camelKYamlDSLParser = new CamelKYamlDSLParser();
			if (camelKYamlDSLParser.getCorrespondingLine(textDocumentItem, line) != null) {
				return camelKYamlDSLParser;
			}
		} else if(isCamelKafkaConnectDSL(textDocumentItem, uri)) {
			CamelKafkaConnectDSLParser camelKafkaConnectDSLParser = new CamelKafkaConnectDSLParser();
			if (camelKafkaConnectDSLParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
				return camelKafkaConnectDSLParser;
			}
		} else if(isCamelKKotlinDSL(textDocumentItem, uri)) {
			CamelKKotlinDSLParser camelKKotlinDSLParser = new CamelKKotlinDSLParser();
			if (camelKKotlinDSLParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
				return camelKKotlinDSLParser;
			}
		} else if(isCamelKJSDSL(textDocumentItem, uri)) {
			CamelKJSDSLParser camelKJSDSLParser = new CamelKJSDSLParser();
			if (camelKJSDSLParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
				return camelKJSDSLParser;
			}
		}
		return null;
	}
	
	private boolean isCamelKJSDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_JS_FILENAME_SUFFIX)
				|| isJSFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isJSFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".js") && textDocumentItem.getText().startsWith(MODELINE_LIKE_CAMEL_K);
	}

	private boolean isCamelKafkaConnectDSL(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".properties")
				&& containsCamelKafkaConnectPropertyKey(textDocumentItem);
	}

	protected boolean containsCamelKafkaConnectPropertyKey(TextDocumentItem textDocumentItem) {
		String text = textDocumentItem.getText();
		return text.contains(CamelKafkaConnectDSLParser.CAMEL_SINK_URL)
				|| text.contains(CamelKafkaConnectDSLParser.CAMEL_SOURCE_URL);
	}

	private boolean isCamelKKotlinDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_KOTLIN_FILENAME_SUFFIX)
				|| isKotlinFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isKotlinFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".kts") && textDocumentItem.getText().startsWith(MODELINE_LIKE_CAMEL_K);
	}

	private boolean isCamelKGroovyDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_GROOVY_FILENAME_SUFFIX)
				|| isGroovyFileWithCamelKShebang(textDocumentItem, uri)
				|| isGroovyFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isGroovyFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".groovy") && textDocumentItem.getText().startsWith(MODELINE_LIKE_CAMEL_K);
	}

	protected boolean isGroovyFileWithCamelKShebang(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".groovy") && textDocumentItem.getText().startsWith(SHEBANG_CAMEL_K);
	}

	private boolean isCamelKYamlDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_YAML_FILENAME_SUFFIX)
				|| isYamlFileWithCamelKShebang(textDocumentItem, uri)
				|| isYamlFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isYamlFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".yaml") && textDocumentItem.getText().startsWith(MODELINE_LIKE_CAMEL_K);
	}

	protected boolean isYamlFileWithCamelKShebang(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".yaml") && textDocumentItem.getText().startsWith(SHEBANG_CAMEL_K);
	}

	private boolean isCamelJavaDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(".java") && textDocumentItem.getText().contains("camel");
	}

}
