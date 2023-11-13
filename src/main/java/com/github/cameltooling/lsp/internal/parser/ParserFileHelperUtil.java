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

public class ParserFileHelperUtil {

	@Deprecated
	public String getLine(TextDocumentItem textDocumentItem, Position position) {
		return getLines(textDocumentItem, position.getLine(), position.getLine());
	}

	@Deprecated
	public String getLine(TextDocumentItem textDocumentItem, int line) {
		return getLines(textDocumentItem.getText(), line, line);
	}

	@Deprecated
	public String getLine(String text, int line) {
		String[] lines = text.split("\\r?\\n", line + 2);
		if (lines.length >= line + 1) {
			return lines[line];
		}
		return null;
	}

	public String getLines(TextDocumentItem textDocumentItem, Position position) {
		return getLines(textDocumentItem, position.getLine(), position.getLine());
	}

	public String getLines(TextDocumentItem textDocumentItem, int startLine, int endLine) {
		return getLines(textDocumentItem.getText(), startLine, endLine);
	}

	public String getLines(String text, int startLine, int endLine) {
		String[] lines = text.split("\\r?\\n", endLine + 2);
		StringBuilder sb = new StringBuilder();
		for (int i = startLine; i <= endLine && i < lines.length; i++) {
			sb.append(lines[i]);
		}
		return sb.toString();
	}
	
}
