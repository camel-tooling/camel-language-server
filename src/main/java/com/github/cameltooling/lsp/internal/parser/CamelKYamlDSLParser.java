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
		if (data.containsKey(URI_KEY) && URI_KEY.length() < characterPosition) {
			camelComponentURI = getNonNullValue(data.get(URI_KEY));
		} else if(data.containsKey(TO_KEY) && data.get(TO_KEY).toString().trim().length()>0) {
			camelComponentURI = getNonNullValue(data.get(TO_KEY));
		}
		return camelComponentURI;
	}

	@Override
	public CamelURIInstance createCamelURIInstance(TextDocumentItem textDocumentItem, Position position,
			String camelComponentUri) {
		CamelURIInstance uriInstance = new CamelURIInstance(repairLostEscapeChars(camelComponentUri), new YamlDSLModelHelper(getCorrespondingType(textDocumentItem, position.getLine())), textDocumentItem);
		int start = getStartCharacterInDocumentOnLinePosition(textDocumentItem, position);
		uriInstance.setStartPositionInDocument(new Position(position.getLine(), start));
		uriInstance.setEndPositionInDocument(new Position(position.getLine(), start+repairLostEscapeChars(camelComponentUri).length()));
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
		String uri = extractUriFromYamlData(line);
		if (uri.length()==0) {
			// empty uri
			return findStartPositionOfEmptyURI(line);
		}
		uri = repairLostEscapeChars(uri);
		return line.indexOf(uri);
	}

	@Override
	public int getPositionInCamelURI(TextDocumentItem textDocumentItem, Position position) {
		String line = parserFileHelperUtil.getLine(textDocumentItem, position.getLine());
		return position.getCharacter() - findStartPositionOfEmptyURI(line);
	}

	private String repairLostEscapeChars(String line) {
		String[] parts = line.split("\"");
		String res = "";
		for (int i = 0; i < parts.length; i++) {
			if (i>0) {
				res += "\\\"";
			}
			res += parts[i];
		}
		return res;
	}

	private int findStartPositionOfEmptyURI(String line) {
		int startPos = line.indexOf(':')+2;
		if (line.indexOf("'", startPos) != -1) {
			// we use single quotes
			return line.indexOf("'", startPos)+1;
		} else if (line.indexOf('"', startPos) != -1) {
			// we use double quotes
			return line.indexOf('"', startPos)+1;
		} else {
			// we don't use any quotes
			return startPos;
		}
	}

	public String getCorrespondingType(TextDocumentItem textDocumentItem, int lineNumber) {
		for (int lineNo = lineNumber; lineNo >=0; lineNo--) {
			String tempLine = parserFileHelperUtil.getLine(textDocumentItem, lineNo);
			Map<?, ?> data = parseYaml(tempLine);
			if (data.containsKey(TO_KEY)) {
				return "to";
			} else if (data.containsKey(FROM_KEY)) {
				return "from";
			} else if (data.containsKey(REST_KEY)) {
				return null;
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
		return getNonNullValue(m.values().toArray()[0]);
	}

	private Map<?, ?> extractMapFromYaml(Object o) {
		if (o instanceof List) {
			List<?> l = (List<?>)o;
			if (l.size()>0 && l.get(0) instanceof Map) {
				return (Map<?, ?>)l.get(0);
			}
		} else if (o instanceof Map) {
			return (Map<?, ?>)o;
		}
		return null;
	}
}
