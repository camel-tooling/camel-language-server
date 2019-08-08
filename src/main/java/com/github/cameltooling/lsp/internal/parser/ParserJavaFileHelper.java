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

import java.util.Arrays;
import java.util.List;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;

public class ParserJavaFileHelper extends ParserFileHelper {
	
	protected static final List<String> CAMEL_POSSIBLE_TYPES = Arrays.asList("to", "from");
	private static final char ENCLOSING_STRING_CHARACTER_FOR_JAVA = '\"';
	
	@Override
	public String getCamelComponentUri(String line, int characterPosition) {
		for (String methodName : CAMEL_POSSIBLE_TYPES) {
			String beforeCamelURI = methodName + "(" + getEnclosingStringCharacter();
			int methodPosition = line.indexOf(beforeCamelURI);
			if (methodPosition != -1) {
				int methodNameOffset = methodName.length() + 2;
				int potentialStartOfCamelURI = methodPosition + methodNameOffset;
				int nextQuote = line.indexOf(getEnclosingStringCharacter(), potentialStartOfCamelURI);
				if (isBetween(characterPosition, potentialStartOfCamelURI, nextQuote)) {
					return line.substring(potentialStartOfCamelURI, nextQuote);
				}
			}
		}
		return null;
	}

	protected char getEnclosingStringCharacter() {
		return ENCLOSING_STRING_CHARACTER_FOR_JAVA;
	}

	public String getCorrespondingMethodName(TextDocumentItem textDocumentItem, int line) {
		String lineString = parserFileHelperUtil.getLine(textDocumentItem, line);
		for (String methodName : CAMEL_POSSIBLE_TYPES) {
			if(lineString.contains(methodName + "(" + getEnclosingStringCharacter())) {
				return methodName;
			}
		}
		return null;
	}

	@Override
	public CamelURIInstance createCamelURIInstance(TextDocumentItem textDocumentItem, Position position, String camelComponentUri) {
		CamelURIInstance uriInstance = new CamelURIInstance(camelComponentUri, getCorrespondingMethodName(textDocumentItem, position.getLine()), textDocumentItem);
		int start = getStartCharacterInDocumentOnLinePosition(textDocumentItem, position);
		uriInstance.setStartPositionInDocument(new Position(position.getLine(), start));
		uriInstance.setEndPositionInDocument(new Position(position.getLine(), start+camelComponentUri.length()));
		return uriInstance;
	}
	
	private int getStartCharacterInDocumentOnLinePosition(TextDocumentItem textDocumentItem, Position position) {
		String beforeCamelURI = getCorrespondingMethodName(textDocumentItem, position.getLine()) + "(" + getEnclosingStringCharacter();
		return parserFileHelperUtil.getLine(textDocumentItem, position).indexOf(beforeCamelURI) + beforeCamelURI.length();
	}

	@Override
	public int getPositionInCamelURI(TextDocumentItem textDocumentItem, Position position) {
		String beforeCamelURI = getCorrespondingMethodName(textDocumentItem, position.getLine()) + "(" + getEnclosingStringCharacter();
		return position.getCharacter() - parserFileHelperUtil.getLine(textDocumentItem, position).indexOf(beforeCamelURI) - beforeCamelURI.length();
	}

}
