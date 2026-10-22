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
package com.github.cameltooling.lsp.internal.completion;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.Position;

/**
 * Line-based view of a Camel YAML DSL document used by completion and hover.
 * <p>
 * Completion and hover are requested while a document is being typed, when it is often not valid YAML,
 * so the structure is derived from indentation instead of being parsed. The text is split only once.
 */
public class YamlDslLines {

	private static final Pattern BLOCK_SCALAR_INDICATOR = Pattern.compile("[|>][-+0-9]*(\\s+#.*)?");

	private final String[] lines;

	public YamlDslLines(String text) {
		this.lines = text.split("\\r?\\n", -1);
	}

	public String line(int lineNumber) {
		return lineNumber >= 0 && lineNumber < lines.length ? lines[lineNumber] : "";
	}

	public int indentation(int lineNumber) {
		String line = line(lineNumber);
		int indentation = 0;
		while (indentation < line.length() && line.charAt(indentation) == ' ') {
			indentation++;
		}
		return indentation;
	}

	public boolean isListItem(int lineNumber) {
		return isSequenceEntry(line(lineNumber).trim());
	}

	/**
	 * @return the column where the content of the line starts, after the indentation and the sequence indicator
	 */
	public int contentColumn(int lineNumber) {
		String line = line(lineNumber);
		int column = indentation(lineNumber);
		if (isListItem(lineNumber)) {
			column++;
			while (column < line.length() && Character.isWhitespace(line.charAt(column))) {
				column++;
			}
		}
		return column;
	}

	/**
	 * @return the content of the line without indentation and without sequence indicator
	 */
	public String content(int lineNumber) {
		String line = line(lineNumber);
		return line.substring(Math.min(contentColumn(lineNumber), line.length())).trim();
	}

	/**
	 * @return the content of the line before the cursor, without indentation and without sequence indicator
	 */
	public String contentBeforeCursor(Position position) {
		String line = line(position.getLine());
		int start = Math.min(contentColumn(position.getLine()), line.length());
		int end = Math.max(start, Math.min(position.getCharacter(), line.length()));
		return line.substring(start, end).trim();
	}

	/**
	 * @return the key declared on the line, or null if the line declares no key
	 */
	public String key(int lineNumber) {
		String content = content(lineNumber);
		int separator = separatorIndex(content);
		return separator > 0 ? unquote(content.substring(0, separator).trim()) : null;
	}

	/**
	 * @return the value written after the key on the line, empty for a block value, or null if the line declares no key
	 */
	public String value(int lineNumber) {
		String content = content(lineNumber);
		int separator = separatorIndex(content);
		return separator > 0 ? content.substring(separator + 1).trim() : null;
	}

	/**
	 * @return the column after the last character of the key declared on the line, or -1 if the line declares no key
	 */
	public int keyEndColumn(int lineNumber) {
		int separator = separatorIndex(content(lineNumber));
		return separator > 0 ? contentColumn(lineNumber) + content(lineNumber).substring(0, separator).trim().length() : -1;
	}

	/**
	 * @return the line declaring the key that the given line is nested in, or -1 for a top-level line.
	 * Sequence entries written at the indentation of their key ("indentless" sequences) are supported.
	 */
	public int parent(int lineNumber) {
		return parent(lineNumber, indentation(lineNumber), isListItem(lineNumber));
	}

	/**
	 * @return the parent of the line where the cursor is. On a blank line, the indentation is the one of the cursor.
	 */
	public int parent(Position position) {
		int lineNumber = position.getLine();
		return parent(lineNumber, indentation(position), isListItem(lineNumber));
	}

	private int indentation(Position position) {
		int lineNumber = position.getLine();
		int indentation = indentation(lineNumber);
		if (line(lineNumber).trim().isEmpty()) {
			indentation = Math.min(indentation, position.getCharacter());
		}
		return indentation;
	}

	private int parent(int lineNumber, int indentation, boolean listItem) {
		for (int candidate = lineNumber - 1; candidate >= 0; candidate--) {
			if (isIgnorable(candidate)) {
				continue;
			}
			int candidateIndentation = indentation(candidate);
			if (candidateIndentation < indentation
					|| candidateIndentation == indentation && listItem && !isListItem(candidate) && isBlockKey(candidate)) {
				return candidate;
			}
		}
		return -1;
	}

	/**
	 * @return the line declaring the key that owns the mapping in which the cursor is, or -1.
	 * For a key written next to the key of a sequence entry, the owner is the key of the sequence.
	 */
	public int owner(Position position) {
		int lineNumber = position.getLine();
		int parent = parent(position);
		if (parent >= 0 && !isListItem(lineNumber) && isListItem(parent) && indentation(position) == contentColumn(parent)) {
			return parent(parent);
		}
		return parent;
	}

	/**
	 * @return the keys of the given line and of the lines owning it, from the top level of the document
	 */
	public List<String> keyPath(int lineNumber) {
		LinkedList<String> keys = new LinkedList<>();
		for (int line = lineNumber; line >= 0; line = owner(new Position(line, 0))) {
			String key = key(line);
			if (key == null) {
				break;
			}
			keys.addFirst(key);
		}
		return keys;
	}

	/**
	 * @return the lines declaring the direct children of the given line
	 */
	public List<Integer> children(int lineNumber) {
		List<Integer> children = new ArrayList<>();
		int keyIndentation = indentation(lineNumber);
		boolean acceptsIndentlessEntries = !isListItem(lineNumber) && isBlockKey(lineNumber);
		int childIndentation = -1;
		for (int candidate = lineNumber + 1; candidate < lines.length; candidate++) {
			if (isIgnorable(candidate)) {
				continue;
			}
			int candidateIndentation = indentation(candidate);
			boolean indentlessEntry = acceptsIndentlessEntries && candidateIndentation == keyIndentation && isListItem(candidate);
			if (candidateIndentation <= keyIndentation && !indentlessEntry) {
				break;
			}
			if (childIndentation == -1) {
				childIndentation = candidateIndentation;
			}
			if (candidateIndentation == childIndentation) {
				children.add(candidate);
			}
		}
		return children;
	}

	/**
	 * @return whether the line at the given position is part of a literal or folded block scalar
	 */
	public boolean isInsideBlockScalar(Position position) {
		for (int ancestor = parent(position); ancestor >= 0; ancestor = parent(ancestor)) {
			String value = value(ancestor);
			if (value != null && BLOCK_SCALAR_INDICATOR.matcher(value).matches()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return whether the line declares a key without inline value, so that its value is a block on the following lines
	 */
	public boolean isBlockKey(int lineNumber) {
		String value = value(lineNumber);
		return value != null && (value.isEmpty() || value.startsWith("#"));
	}

	private boolean isIgnorable(int lineNumber) {
		String trimmed = line(lineNumber).trim();
		return trimmed.isEmpty() || trimmed.startsWith("#");
	}

	private static boolean isSequenceEntry(String trimmedLine) {
		return "-".equals(trimmedLine) || trimmedLine.startsWith("- ") || trimmedLine.startsWith("-\t");
	}

	/**
	 * @return the index of the key/value separator in the given line content, or -1 if there is none
	 */
	public static int separatorIndex(String content) {
		int start = 0;
		if (!content.isEmpty() && (content.charAt(0) == '"' || content.charAt(0) == '\'')) {
			start = content.indexOf(content.charAt(0), 1);
			if (start == -1) {
				return -1;
			}
		}
		for (int i = start; i < content.length(); i++) {
			char c = content.charAt(i);
			if (c == '#' && i > 0 && Character.isWhitespace(content.charAt(i - 1))) {
				return -1;
			}
			if (c == ':' && (i + 1 == content.length() || Character.isWhitespace(content.charAt(i + 1)))) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * @return the value without the enclosing quotes and without trailing comment
	 */
	public static String unquote(String value) {
		if (value.length() >= 2 && (value.charAt(0) == '"' || value.charAt(0) == '\'')) {
			int closingQuote = value.indexOf(value.charAt(0), 1);
			if (closingQuote > 0) {
				return value.substring(1, closingQuote);
			}
		}
		int comment = value.indexOf(" #");
		return comment >= 0 ? value.substring(0, comment).trim() : value;
	}
}
