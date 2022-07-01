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

import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineFileType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Parses and checks text document to see if camel-k modeline insertion completions are available.
 *
 * @author joshiraez
 */
public class CamelKModelineInsertionParser {

	private final TextDocumentItem document;

	public CamelKModelineInsertionParser(TextDocumentItem document) {
		this.document = document;
	}

	public boolean canPutCamelKModeline(Position position) {
		int currentLine = position.getLine();

		return isCamelKFile()
				&& lineIsEmpty(currentLine)
				&& noModelineInsertedAlready()
				&& previousLinesAreCommentsOrEmpty(currentLine);
	}

	private boolean isCamelKFile() {
		return CamelKModelineFileType.getFileTypeCorrespondingToUri(document.getUri()).isPresent();
	}

	private boolean lineIsEmpty(int line) {
		return new ParserFileHelperUtil().getLine(document, line).isBlank();
	}

	private boolean noModelineInsertedAlready() {
		String modeline = CamelKModelineFileType.getFileTypeCorrespondingToUri(document.getUri()).orElseThrow()
				.getModeline();

		return !Arrays.stream(document.getText().split("\n")).anyMatch(line -> line.startsWith(modeline));
	}

	private boolean previousLinesAreCommentsOrEmpty(int line) {
		String textBeforeLine = IntStream.range(0, line).boxed()
				.map(currLine -> new ParserFileHelperUtil().getLine(document,currLine))
				.collect(Collectors.joining("\n"));

		return textIsFullOfRegex(textBeforeLine,
				CamelKModelineFileType.getFileTypeCorrespondingToUri(document.getUri()).orElseThrow()
						.getCommentRegexSupplier().get());
	}
	private boolean textIsFullOfRegex(String text, Pattern regex) {
		//Add an extra carriage return at the end for correct matching with line comments
		String textWithExtraCarriageReturn = text + "\n";
		String textWithoutComments = textWithExtraCarriageReturn.replaceAll(regex.pattern(), "");

		return textWithoutComments.isBlank();
	}
}
