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

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.YamlDSLModelHelper;

/**
 * @author Lars Heinemann
 */
public class CamelKYamlDSLParser extends ParserFileHelper {
	
	public static final String URI_KEY = "uri";
	public static final String REST_KEY = "rest";
	public static final String FROM_KEY = "from";
	public static final String TO_KEY = "to";

	@Override
	public String getCamelComponentUri(String line, int characterPosition) {
		String camelComponentURI = null;
		Map<?, ?> data = parseYaml(line);
		if (data != null && data.containsKey(URI_KEY) && URI_KEY.length() < characterPosition) {
			camelComponentURI = getNonNullValue(data.get(URI_KEY));
		} else if(data != null && data.containsKey(TO_KEY) && data.get(TO_KEY).toString().trim().length()>0) {
			camelComponentURI = getNonNullValue(data.get(TO_KEY));
		}
		return camelComponentURI;
	}

	@Override
	public CamelURIInstance createCamelURIInstance(TextDocumentItem textDocumentItem, Position position,
			String camelComponentUri) {
		String line = parserFileHelperUtil.getLine(textDocumentItem, position.getLine());
		String stringEncloser = getStringEncloser(line);
		CamelURIInstance uriInstance = new CamelURIInstance(repairLostEscapeChars(stringEncloser, camelComponentUri), new YamlDSLModelHelper(getCorrespondingType(textDocumentItem, position.getLine())), textDocumentItem);
		int start = getStartCharacterInDocumentOnLinePosition(textDocumentItem, position);
		uriInstance.setStartPositionInDocument(new Position(position.getLine(), start));
		uriInstance.setEndPositionInDocument(new Position(position.getLine(), start+repairLostEscapeChars(stringEncloser, camelComponentUri).length()));
		return uriInstance;
	}

	private String getNonNullValue(Object o) {
		if (o != null) {
			return o.toString();
		}
		return "";
	}

	private int getStartCharacterInDocumentOnLinePosition(TextDocumentItem textDocumentItem, Position position) {
		String line = parserFileHelperUtil.getLine(textDocumentItem, position.getLine());
		String stringEncloser = getStringEncloser(line);
		String uri = extractUriFromYamlData(line);
		if (uri == null || uri.isEmpty()) {
			// empty uri
			return findStartPositionOfURI(line);
		}
		uri = repairLostEscapeChars(stringEncloser, uri);
		return line.indexOf(uri);
	}

	@Override
	public int getPositionInCamelURI(TextDocumentItem textDocumentItem, Position position) {
		String line = parserFileHelperUtil.getLine(textDocumentItem, position.getLine());
		return position.getCharacter() - findStartPositionOfURI(line);
	}

	private String getStringEncloser(String line) {
		int idx = determineQuoteStartPos(line, line.indexOf(':'));
		if (idx != -1) {
			String stringEncloser = String.valueOf(line.charAt(idx));
			if (stringEncloser.equals("\"") || stringEncloser.equals("'")) {
				return stringEncloser;
			}
		}
		return null;
	}

	String repairLostEscapeChars(String stringEncloser, String line) {
		if (stringEncloser == null) {
			return line;
		}
		if(line == null) {
			return "";
		}
		String[] parts = line.split(stringEncloser);
		StringBuilder res = new StringBuilder();
		for (int i = 0; i < parts.length; i++) {
			if (i>0) {
				if ("\"".equals(stringEncloser)) {
					res.append(String.format("\\%s", stringEncloser));
				} else {
					res.append(String.format("%s%s", stringEncloser, stringEncloser));
				}
			}
			res.append(parts[i]);
		}
		return res.toString();
	}

	private int findStartPositionOfURI(String line) {
		int separatorPos = line.indexOf(':')+1;
		int start = determineQuoteStartPos(line, separatorPos);
		if (isStartOfUri(separatorPos, start, line)) {
			return start + 1;
		} else {
			int spaceCnt = 0;
			for (int i = separatorPos; i<line.length(); i++) {
				if (line.charAt(i) == ' ') {
					spaceCnt++;
				} else {
					break;
				}
			}
			return separatorPos + spaceCnt;
		}
	}

	private int determineQuoteStartPos(String line, int separatorPos) {
		int singleQuotePos = line.indexOf('\'', separatorPos);
		int doubleQuotePos = line.indexOf('"', separatorPos);
		int startPos = -1;
		if (singleQuotePos == -1) {
			if (doubleQuotePos != -1) {
				startPos = doubleQuotePos;
			}
		} else {
			if (doubleQuotePos == -1) {
				startPos = singleQuotePos;
			} else {
				startPos = Math.min(singleQuotePos, doubleQuotePos);
			}
		}
		if (startPos != -1 && startPos > separatorPos) {
			for (int i = startPos; i > separatorPos; i--) {
				if (line.charAt(i) != '"' && line.charAt(i) != '\'' && line.charAt(i) != ' ') {
					// there should no other char next to a string encloser, otherwise we might be in plain mode
					return -1;
				}
			}
		}
		return startPos;
	}

	private boolean isStartOfUri(int separatorPos, int pos, String line) {
		if (pos == -1) {
			return false;
		}
		for (int i = pos-1; i > separatorPos; i--) {
			if (line.charAt(i) != ' ' && line.charAt(i) != ':') {
				return false;
			}
		}
		return true;
	}

	public String getCorrespondingType(TextDocumentItem textDocumentItem, int lineNumber) {
		for (int lineNo = lineNumber; lineNo >=0; lineNo--) {
			String tempLine = parserFileHelperUtil.getLine(textDocumentItem, lineNo);
			Map<?, ?> data = parseYaml(tempLine);
			if (data != null) {
				if (data.containsKey(TO_KEY)) {
					return "to";
				} else if (data.containsKey(FROM_KEY)) {
					return "from";
				} else if (data.containsKey(REST_KEY)) {
					return null;
				}
			}
		}
		return null;
	}

	private Map<?, ?> parseYaml(String line) {
		Yaml yaml = new Yaml();
		Object obj = yaml.load(line);
		return extractMapFromYaml(obj);
	}

	private String extractUriFromYamlData(String line) {
		Yaml yaml = new Yaml();
		Object obj = yaml.load(line);
		Map<?, ?> m = extractMapFromYaml(obj);
		if (m != null && !m.values().isEmpty()) {
			return getNonNullValue(m.values().toArray()[0]);
		}
		return null;
	}

	private Map<?, ?> extractMapFromYaml(Object o) {
		if (o instanceof List) {
			List<?> l = (List<?>)o;
			if (!l.isEmpty() && l.get(0) instanceof Map) {
				return (Map<?, ?>)l.get(0);
			}
		} else if (o instanceof Map) {
			return (Map<?, ?>)o;
		}
		return null;
	}
}
