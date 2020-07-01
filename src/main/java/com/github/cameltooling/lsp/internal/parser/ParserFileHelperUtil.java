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

	public String getLine(TextDocumentItem textDocumentItem, Position position) {
		return getLine(textDocumentItem, position.getLine());
	}
	
	public String getLine(TextDocumentItem textDocumentItem, int line) {
		return getLine(textDocumentItem.getText(), line);
	}

	public String getLine(String text, int line) {
		String[] lines = text.split("\\r?\\n", line + 2);
		if (lines.length >= line + 1) {
			return lines[line];
		}
		return null;
	}
	
}
