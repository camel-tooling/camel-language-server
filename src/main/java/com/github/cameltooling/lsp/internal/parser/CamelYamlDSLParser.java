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
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.List;
import java.util.Map;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.YamlDSLModelHelper;

/**
 * @author Lars Heinemann
 */
public class CamelYamlDSLParser extends ParserFileHelper {
	
	public static final String URI_KEY = "uri";
	public static final String FROM_KEY = "from";
	public static final String TO_KEY = "to";

	public static final String[] URI_SEARCH = new String[]{URI_KEY, "- " + TO_KEY};

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
	public String getCamelComponentUri(TextDocumentItem textDocumentItem, Position position) {
		StringBuilder lines = new StringBuilder();
		int pos = extractLines(textDocumentItem, position, URI_SEARCH, lines, position.getCharacter(), false);
		return getCamelComponentUri(lines.toString(),  pos);
	}


	@Override
	public CamelURIInstance createCamelURIInstance(TextDocumentItem textDocumentItem, Position position,
			String camelComponentUri) {
		StringBuilder sb = new StringBuilder();
		extractLines(textDocumentItem, position, URI_SEARCH, sb, position.getCharacter(), false);
		String line = sb.toString();
		String stringEncloser = getStringEncloser(line);
		CamelURIInstance uriInstance = new CamelURIInstance(repairLostEscapeChars(stringEncloser, camelComponentUri), new YamlDSLModelHelper(getCorrespondingType(textDocumentItem, position.getLine())), textDocumentItem);
		int start = getStartCharacterInDocumentOnLinePosition(line);
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

	private int getStartCharacterInDocumentOnLinePosition(String line) {
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
		StringBuilder lines = new StringBuilder();
		int pos = extractLines(textDocumentItem, position, URI_SEARCH, lines, position.getCharacter(), true);
		String line = lines.toString();
		return pos - findStartPositionOfURI(line);
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
		StringBuilder sb = new StringBuilder();
		extractLines(textDocumentItem, new Position(lineNumber, 0), "-", sb, 0, false);

		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
		return getCorrespondingTypeFromUnknownElement(yaml.load(sb.toString()));
	}

	private static String getCorrespondingTypeFromUnknownElement(Object yamlElement) {
		String type = null;

		//With multi-line, we may be looking at a bigger picture here
		if(yamlElement instanceof List list) {
			for(var e : list) {
				type = getCorrespondingTypeFromUnknownElement(e);
				if(type != null) {
					break;
				}
			}
		} else if (yamlElement instanceof Map map) {
			if (map.containsKey(TO_KEY)) {
				type = "to";
			} else if (map.containsKey(FROM_KEY)) {
				type = "from";
			} else {
				for (var entry : map.values()) {
					type = getCorrespondingTypeFromUnknownElement(entry);
					if(type != null) {
						break;
					}
				}
			}
		}

		return type;
	}

	private Map<?, ?> parseYaml(String line) {
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
		Object obj = yaml.load(line);
		return extractMapFromYaml(obj);
	}

	private String extractUriFromYamlData(String line) {
		Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
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

	private int extractLines(TextDocumentItem textDocumentItem, Position position, String whenToStop,
							 StringBuilder lines, int pos, boolean cleanNewLine) {
		return extractLines(textDocumentItem, position, new String[]{whenToStop}, lines, pos, cleanNewLine);
	}

	private int extractLines(TextDocumentItem textDocumentItem, Position position, String[] whenToStopArray,
							 StringBuilder lines, int pos, boolean cleanNewLine) {
		// If it is a multi-line, cleanNewLine decides if we convert it to one very long single line
		for (Integer lineNo = position.getLine(); lineNo >=0; lineNo--) {
			String tempLine = parserFileHelperUtil.getLines(textDocumentItem, lineNo, lineNo);

			for (String whenToStop : whenToStopArray) {
				if (tempLine.stripLeading().startsWith(whenToStop)) {
					// Remove the potential '>' character of multiline
					if (tempLine.stripLeading().substring(whenToStop.length() + 1).stripLeading().startsWith(">")) {
						tempLine = tempLine.substring(0, tempLine.indexOf(">"));
					}
					lineNo = -1; // Stop after this iteration, do not continue the loop
					break;
				}
			}

			// Prepend the content
			if (cleanNewLine) {
				// If it is multiline, we have to move the position forward
				if (lines.length() > 0) {
					final var tempLineStripped = tempLine.stripLeading();
					pos += tempLineStripped.length();
					lines.insert(0, tempLineStripped);
				} else {
					lines.insert(0, tempLine);
				}
			} else {
				lines.insert(0, tempLine);
				if (lineNo >=0) {
					lines.insert(0, "\n");
				}
			}
		}
		return pos;
	}
}
