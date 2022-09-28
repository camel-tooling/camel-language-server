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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.lsp4j.TextDocumentItem;
import org.xml.sax.SAXException;

public class ParserFileHelperFactory {
	
	private static final String KUBERNETES_CRD_API_VERSION_CAMEL = "apiVersion: camel.apache.org/";
	private static final String CAMELK_XML_FILENAME_SUFFIX = "camelk.xml";
	private static final String CAMELK_GROOVY_FILENAME_SUFFIX = ".camelk.groovy";
	private static final String CAMELK_KOTLIN_FILENAME_SUFFIX = ".camelk.kts";
	private static final String CAMELK_YAML_FILENAME_SUFFIX = ".camelk.yaml";
	private static final String PLAIN_CAMEL_YAML_FILENAME_SUFFIX = ".camel.yaml";
	private static final String CAMELK_JS_FILENAME_SUFFIX = ".camelk.js";
	private static final String SHEBANG_CAMEL_K = "#!/usr/bin/env camel-k";

	public ParserFileHelper getCorrespondingParserFileHelper(TextDocumentItem textDocumentItem, int line) {
		ParserXMLFileHelper xmlParser = new ParserXMLFileHelper();
		String uri = textDocumentItem.getUri();
		if (uri.endsWith(".xml") && xmlParser.getCorrespondingCamelNodeForCompletion(textDocumentItem, line) != null) {
			return xmlParser;
		} else if(isPotentiallyCamelJavaDSL(textDocumentItem, uri)) {
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
			if (camelKYamlDSLParser.getCorrespondingType(textDocumentItem, line) != null) {
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

	/**
	 * @param textDocumentItem the Document to check if it is a Camel one or not
	 * @return if it is most probably a Camel file. "Probably" because the heuristic is far from perfect. But it is the best that we have already implemented in the Language Server.
	 */
	public boolean isProbablyCamelFile(TextDocumentItem textDocumentItem) {
		String uri = textDocumentItem.getUri();
		return isHighProbabilityCamelJavaDSL(textDocumentItem, uri)
				|| isCamelXMLDSL(textDocumentItem, uri)
				|| isCamelKJSDSL(textDocumentItem, uri)
				|| isCamelKYamlDSL(textDocumentItem, uri)
				|| isCamelKKotlinDSL(textDocumentItem, uri)
				|| isCamelKGroovyDSL(textDocumentItem, uri)
				|| isCamelKafkaConnectDSL(textDocumentItem, uri);
	}

	private boolean isCamelXMLDSL(TextDocumentItem textDocumentItem, String uri) {
		try {
			return uri.endsWith(CAMELK_XML_FILENAME_SUFFIX)
					|| uri.endsWith(".xml") && new ParserXMLFileHelper().hasElementFromCamelNamespace(textDocumentItem);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			return false;
		}
	}

	private boolean isCamelKJSDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_JS_FILENAME_SUFFIX)
				|| isJSFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isJSFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".js") && textDocumentItem.getText().startsWith(CamelKModelineParser.MODELINE_LIKE_CAMEL_K);
	}

	private boolean isCamelKafkaConnectDSL(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".properties")
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
		return uri.endsWith(".kts") && textDocumentItem.getText().startsWith(CamelKModelineParser.MODELINE_LIKE_CAMEL_K);
	}

	private boolean isCamelKGroovyDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_GROOVY_FILENAME_SUFFIX)
				|| isGroovyFileWithCamelKShebang(textDocumentItem, uri)
				|| isGroovyFileWithCamelKModelineLike(textDocumentItem, uri);
	}

	private boolean isGroovyFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".groovy") && textDocumentItem.getText().startsWith(CamelKModelineParser.MODELINE_LIKE_CAMEL_K);
	}

	protected boolean isGroovyFileWithCamelKShebang(TextDocumentItem textDocumentItem, String uri) {
		return uri.endsWith(".groovy") && textDocumentItem.getText().startsWith(SHEBANG_CAMEL_K);
	}

	private boolean isCamelKYamlDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_YAML_FILENAME_SUFFIX)
				|| uri.endsWith(PLAIN_CAMEL_YAML_FILENAME_SUFFIX)
				|| isYamlFileWithCamelKShebang(textDocumentItem, uri)
				|| isYamlFileWithCamelKModelineLike(textDocumentItem, uri)
				|| isYamlFileOfCRDType(textDocumentItem, uri);
	}

	private boolean isYamlFileOfCRDType(TextDocumentItem textDocumentItem, String uri) {
		return hasYamlExtension(uri)
				&& textDocumentItem.getText().startsWith(KUBERNETES_CRD_API_VERSION_CAMEL);
	}

	private boolean hasYamlExtension(String uri) {
		return uri.endsWith(".yaml") || uri.endsWith(".yml");
	}

	private boolean isYamlFileWithCamelKModelineLike(TextDocumentItem textDocumentItem, String uri) {
		return hasYamlExtension(uri) && textDocumentItem.getText().startsWith(CamelKModelineParser.MODELINE_LIKE_CAMEL_K_YAML);
	}

	protected boolean isYamlFileWithCamelKShebang(TextDocumentItem textDocumentItem, String uri) {
		return hasYamlExtension(uri) && textDocumentItem.getText().startsWith(SHEBANG_CAMEL_K);
	}

	private boolean isPotentiallyCamelJavaDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(".java") && textDocumentItem.getText().contains("camel");
	}
	
	private boolean isHighProbabilityCamelJavaDSL(TextDocumentItem textDocumentItem, String uri) {
		String text = textDocumentItem.getText();
		return isPotentiallyCamelJavaDSL(textDocumentItem, uri)
				&& text.contains("from(")
				&& text.contains(".to");
	}

}
