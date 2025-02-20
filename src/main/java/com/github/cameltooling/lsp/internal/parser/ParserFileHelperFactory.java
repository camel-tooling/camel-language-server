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

import com.github.cameltooling.lsp.internal.parser.fileparserhelper.ParserChainOfResponsibility;
import org.eclipse.lsp4j.TextDocumentItem;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class ParserFileHelperFactory {
	
	private static final String KUBERNETES_CRD_API_VERSION_CAMEL = "apiVersion: camel.apache.org/";
	private static final String CAMELK_XML_FILENAME_SUFFIX = "camelk.xml";
	private static final String CAMELK_YAML_FILENAME_SUFFIX = ".camelk.yaml";
	private static final String PLAIN_CAMEL_YAML_FILENAME_SUFFIX = ".camel.yaml";
	private static final String CAMELK_YML_FILENAME_SUFFIX = ".camelk.yml";
	private static final String PLAIN_CAMEL_YML_FILENAME_SUFFIX = ".camel.yml";
	private static final String SHEBANG_CAMEL_K = "#!/usr/bin/env camel-k";

	public ParserFileHelper getCorrespondingParserFileHelper(TextDocumentItem textDocumentItem, int line) {
		return ParserChainOfResponsibility.getMatchedParserFileHelper(textDocumentItem, line, this);
	}

	/**
	 * @param textDocumentItem the Document to check if it is a Camel one or not
	 * @return if it is most probably a Camel file. "Probably" because the heuristic is far from perfect. But it is the best that we have already implemented in the Language Server.
	 */
	public boolean isProbablyCamelFile(TextDocumentItem textDocumentItem) {
		String uri = textDocumentItem.getUri();
		return isHighProbabilityCamelJavaDSL(textDocumentItem, uri)
				|| isCamelXMLDSL(textDocumentItem, uri)
				|| isCamelYamlDSL(textDocumentItem, uri);
	}

	private boolean isCamelXMLDSL(TextDocumentItem textDocumentItem, String uri) {
		try {
			return uri.endsWith(CAMELK_XML_FILENAME_SUFFIX)
					|| uri.endsWith(".xml") && new ParserXMLFileHelper().hasElementFromCamelNamespace(textDocumentItem);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			return false;
		}
	}

	public boolean isCamelYamlDSL(TextDocumentItem textDocumentItem, String uri) {
		//improve this method to provide better heuristic to detect if it is a Camel file or not
		return uri.endsWith(CAMELK_YAML_FILENAME_SUFFIX)
				|| uri.endsWith(CAMELK_YML_FILENAME_SUFFIX)
				|| uri.endsWith(PLAIN_CAMEL_YAML_FILENAME_SUFFIX)
				|| uri.endsWith(PLAIN_CAMEL_YML_FILENAME_SUFFIX)
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

	public boolean isPotentiallyCamelJavaDSL(TextDocumentItem textDocumentItem, String uri) {
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
