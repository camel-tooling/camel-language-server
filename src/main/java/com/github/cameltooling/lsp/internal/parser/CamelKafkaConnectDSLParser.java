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
import java.io.StringReader;
import java.util.Properties;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.PropertiesDSLModelHelper;

/**
 * @author Aurelien Pupier
 * Restriction on notation for properties file, it must be with '=' and without spaces
 */
public class CamelKafkaConnectDSLParser extends ParserFileHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelKafkaConnectDSLParser.class);

	@Override
	public String getCamelComponentUri(String line, int characterPosition) {
		if (new CamelKafkaUtil().isInsideACamelUri(line, characterPosition)) {
			Properties properties = new Properties();
			try {
				properties.load(new StringReader(line));

				String camelComponentURI = properties.getProperty(CamelKafkaUtil.CAMEL_SOURCE_URL);
				if (camelComponentURI != null) {
					return camelComponentURI;
				} else {
					return properties.getProperty(CamelKafkaUtil.CAMEL_SINK_URL);
				}
			} catch (IOException e) {
				LOGGER.warn("Cannot retrieve Camel Component URI",e);
			}
		}
		return null;
	}

	@Override
	public CamelURIInstance createCamelURIInstance(TextDocumentItem textDocumentItem, Position position,
			String camelComponentUri) {
		CamelURIInstance uriInstance = new CamelURIInstance(camelComponentUri, new PropertiesDSLModelHelper(getCorrespondingMethodName(textDocumentItem, position.getLine())), textDocumentItem);
		int start = getStartCharacterInDocumentOnLinePosition(textDocumentItem, position);
		uriInstance.setStartPositionInDocument(new Position(position.getLine(), start));
		uriInstance.setEndPositionInDocument(new Position(position.getLine(), start+camelComponentUri.length()));
		return uriInstance;
	}

	private int getStartCharacterInDocumentOnLinePosition(TextDocumentItem textDocumentItem, Position position) {
		String line = parserFileHelperUtil.getLine(textDocumentItem, position.getLine());
		return line.indexOf('=') + 1;
	}

	@Override
	public int getPositionInCamelURI(TextDocumentItem textDocumentItem, Position position) {
		String line = parserFileHelperUtil.getLine(textDocumentItem, position.getLine());
		return position.getCharacter() - line.indexOf('=') - 1;
	}

	public String getCorrespondingMethodName(TextDocumentItem textDocumentItem, int lineNumber) {
		String line = parserFileHelperUtil.getLine(textDocumentItem, lineNumber);
		if (line.startsWith(CamelKafkaUtil.CAMEL_SINK_URL)) {
			return "to";
		} else if(line.startsWith(CamelKafkaUtil.CAMEL_SOURCE_URL)) {
			return "from";
		}
		return null;
	}

}
