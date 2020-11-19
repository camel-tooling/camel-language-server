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
	
	
	private static final String GROOVY_FILENAME_SUFFIX = ".groovy";
	private static final String JAVA_FILENAME_SUFFIX = ".java";
	private static final String JS_FILENAME_SUFFIX = ".js";
	private static final String KOTLIN_FILENAME_SUFFIX = ".kts";
	private static final String YAML_FILENAME_SUFFIX = ".yaml";
	private static final String XML_FILENAME_SUFFIX = ".xml";
	private static final String CAMELK_GROOVY_FILENAME_SUFFIX = ".camelk.groovy";
	private static final String CAMELK_KOTLIN_FILENAME_SUFFIX = ".camelk.kts";
	private static final String CAMELK_YAML_FILENAME_SUFFIX = ".camelk.yaml";
	private static final String CAMELK_JS_FILENAME_SUFFIX = ".camelk.js";
	private static final String CAMEL_KAFKA_CONNECT_FILENAME_SUFFIX = ".properties";
	private static final String SHEBANG_CAMEL_K = "#!/usr/bin/env camel-k";

	public ParserFileHelper getCorrespondingParserFileHelper(TextDocumentItem textDocumentItem, int line) {
		ParserFileHelper parser = null;
		String uri = textDocumentItem.getUri();

		if (isCamelXmlDSL(uri)) {
			parser = getXmlDslParser(textDocumentItem, line);
		} else if (isCamelJavaDSL(textDocumentItem, uri)) {
			parser = getJavaDslParser(textDocumentItem, line);
		} else if (isCamelKGroovyDSL(textDocumentItem, uri)) {
			parser = getCamelKGroovyDslParser(textDocumentItem, line);
		} else if (isCamelKYamlDSL(textDocumentItem, uri)) {
			parser = getCamelKYamlDslParser(textDocumentItem, line);
		} else if (isCamelKKotlinDSL(textDocumentItem, uri)) {
			parser = getCamelKKotlinDslParser(textDocumentItem, line);
		} else if (isCamelKJSDSL(textDocumentItem, uri)) {
			parser = getCamelKJSDslParser(textDocumentItem, line);
		} else if (isCamelKafkaConnectDSL(textDocumentItem, uri)) {
			parser = getCamelKafkaConnectDslParser(textDocumentItem, line);
		}

		return parser;
	}
	
	private boolean isCamelKJSDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_JS_FILENAME_SUFFIX)
				|| isJSFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isJSFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(JS_FILENAME_SUFFIX) && textDocumentItem.getText().startsWith(CamelKModelineParser.MODELINE_LIKE_CAMEL_K);
	}

	private boolean isCamelKafkaConnectDSL(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(CAMEL_KAFKA_CONNECT_FILENAME_SUFFIX)
				&& containsCamelKafkaConnectPropertyKey(textDocumentItem);
	}

	protected boolean containsCamelKafkaConnectPropertyKey(TextDocumentItem textDocumentItem) {
		String text = textDocumentItem.getText();
		return text.contains(CamelKafkaUtil.CAMEL_SINK_URL)
				|| text.contains(CamelKafkaUtil.CAMEL_SOURCE_URL);
	}

	private boolean isCamelKKotlinDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_KOTLIN_FILENAME_SUFFIX)
				|| isKotlinFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isKotlinFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(KOTLIN_FILENAME_SUFFIX) && textDocumentItem.getText().startsWith(CamelKModelineParser.MODELINE_LIKE_CAMEL_K);
	}

	private boolean isCamelKGroovyDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_GROOVY_FILENAME_SUFFIX)
				|| isGroovyFileWithCamelKShebang(textDocumentItem, uri)
				|| isGroovyFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isGroovyFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(GROOVY_FILENAME_SUFFIX) && textDocumentItem.getText().startsWith(CamelKModelineParser.MODELINE_LIKE_CAMEL_K);
	}

	protected boolean isGroovyFileWithCamelKShebang(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(GROOVY_FILENAME_SUFFIX) && textDocumentItem.getText().startsWith(SHEBANG_CAMEL_K);
	}

	private boolean isCamelKYamlDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_YAML_FILENAME_SUFFIX)
				|| isYamlFileWithCamelKShebang(textDocumentItem, uri)
				|| isYamlFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isYamlFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(YAML_FILENAME_SUFFIX) && textDocumentItem.getText().startsWith(CamelKModelineParser.MODELINE_LIKE_CAMEL_K_YAML);
	}

	protected boolean isYamlFileWithCamelKShebang(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(YAML_FILENAME_SUFFIX) && textDocumentItem.getText().startsWith(SHEBANG_CAMEL_K);
	}

	private boolean isCamelJavaDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(JAVA_FILENAME_SUFFIX) && textDocumentItem.getText().contains("camel");
	}

	private boolean isCamelXmlDSL(String uri) {
		return uri.endsWith(XML_FILENAME_SUFFIX);
	}

	private ParserFileHelper getXmlDslParser(TextDocumentItem textDocumentItem, int line) {
		ParserXMLFileHelper parser = new ParserXMLFileHelper();
		if (parser.getCorrespondingCamelNodeForCompletion(textDocumentItem, line) != null) {
			return parser;
		}
		return null;
	}

	private ParserFileHelper getJavaDslParser(TextDocumentItem textDocumentItem, int line) {
		ParserJavaFileHelper javaParser = new ParserJavaFileHelper();
		if (javaParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
			return javaParser;
		}
		return null;
	}

	private ParserFileHelper getCamelKGroovyDslParser(TextDocumentItem textDocumentItem, int line) {
		CamelKGroovyDSLParser camelKGroovyDSLParser = new CamelKGroovyDSLParser();
		if (camelKGroovyDSLParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
			return camelKGroovyDSLParser;
		}
		return null;
	}

	private ParserFileHelper getCamelKYamlDslParser(TextDocumentItem textDocumentItem, int line) {
		CamelKYamlDSLParser camelKYamlDSLParser = new CamelKYamlDSLParser();
		if (camelKYamlDSLParser.getCorrespondingType(textDocumentItem, line) != null) {
			return camelKYamlDSLParser;
		}
		return null;
	}

	private ParserFileHelper getCamelKKotlinDslParser(TextDocumentItem textDocumentItem, int line) {
		CamelKKotlinDSLParser camelKKotlinDSLParser = new CamelKKotlinDSLParser();
		if (camelKKotlinDSLParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
			return camelKKotlinDSLParser;
		}
		return null;
	}

	private ParserFileHelper getCamelKJSDslParser(TextDocumentItem textDocumentItem, int line) {
		CamelKJSDSLParser camelKJSDSLParser = new CamelKJSDSLParser();
		if (camelKJSDSLParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
			return camelKJSDSLParser;
		}
		return null;
	}

	private ParserFileHelper getCamelKafkaConnectDslParser(TextDocumentItem textDocumentItem, int line) {
		CamelKafkaConnectDSLParser camelKafkaConnectDSLParser = new CamelKafkaConnectDSLParser();
		if (camelKafkaConnectDSLParser.getCorrespondingMethodName(textDocumentItem, line) != null) {
			return camelKafkaConnectDSLParser;
		}
		return null;
	}
}
