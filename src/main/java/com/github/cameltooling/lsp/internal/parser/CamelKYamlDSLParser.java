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
package com.github.cameltooling.lsp.internal.parser;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.YamlDSLModelHelper;

/**
 * @author Lars Heinemann
 */
public class CamelKYamlDSLParser extends ParserFileHelper {
	
	public static final String STEP_KEY = "steps:";
	public static final String URI_KEY = "uri:";
	public static final String REST_KEY = "rest:";
	public static final String FROM_KEY = "- from:";
	public static final String TO_KEY = "- to:";

	@Override
	public String getCamelComponentUri(String line, int characterPosition) {
		String camelComponentURI = null;
		String trLine = line.trim();
		if (trLine.startsWith(URI_KEY) && URI_KEY.length() < characterPosition) {
			camelComponentURI = trLine.substring(trLine.indexOf(URI_KEY)+ URI_KEY.length()).trim().replaceAll("\"", "");
		} else if(trLine.startsWith(FROM_KEY) && trLine.indexOf('"') >= trLine.indexOf(':') + 1) {
			camelComponentURI = trLine.substring(trLine.indexOf(FROM_KEY)+ FROM_KEY.length()).trim().replaceAll("\"", "");
		} else if(trLine.startsWith(TO_KEY) && trLine.indexOf('"') >= trLine.indexOf(':') + 1) {
			camelComponentURI = trLine.substring(trLine.indexOf(TO_KEY)+ TO_KEY.length()).trim().replaceAll("\"", "");
		}
		return camelComponentURI;
	}

	@Override
	public CamelURIInstance createCamelURIInstance(TextDocumentItem textDocumentItem, Position position,
			String camelComponentUri) {
		CamelURIInstance uriInstance = new CamelURIInstance(camelComponentUri, new YamlDSLModelHelper(getCorrespondingLine(textDocumentItem, position.getLine())), textDocumentItem);
		int start = getStartCharacterInDocumentOnLinePosition(textDocumentItem, position);
		uriInstance.setStartPositionInDocument(new Position(position.getLine(), start));
		uriInstance.setEndPositionInDocument(new Position(position.getLine(), start+camelComponentUri.length()));
		return uriInstance;
	}

	private int getStartCharacterInDocumentOnLinePosition(TextDocumentItem textDocumentItem, Position position) {
		String line = parserFileHelperUtil.getLine(textDocumentItem, position.getLine());
		return line.indexOf('"') + 1;
	}

	@Override
	public int getPositionInCamelURI(TextDocumentItem textDocumentItem, Position position) {
		String line = parserFileHelperUtil.getLine(textDocumentItem, position.getLine());
		return position.getCharacter() - line.indexOf('"')-1;
	}

	public String getCorrespondingLine(TextDocumentItem textDocumentItem, int lineNumber) {
		for (int lineNo = lineNumber; lineNo >=0; lineNo--) {
			String tempLine = parserFileHelperUtil.getLine(textDocumentItem, lineNo).trim();
			if (tempLine.startsWith(TO_KEY)) {
				return "to";
			} else if (tempLine.startsWith(FROM_KEY)) {
				return "from";
			} else if (tempLine.startsWith(REST_KEY)) {
				return null;			
			}
		}
		return null;
	}
}
