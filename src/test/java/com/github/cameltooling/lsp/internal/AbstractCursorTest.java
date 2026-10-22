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
package com.github.cameltooling.lsp.internal;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;

/**
 * Base class for the tests requesting completion or hover at the position of the {@link #CURSOR} in a document.
 */
public abstract class AbstractCursorTest extends AbstractCamelLanguageServerTest {

	protected static final String CURSOR = "§";

	protected List<CompletionItem> getCompletions(String textWithCursor, String suffixFileName) throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServer(textWithCursor.replace(CURSOR, ""), suffixFileName);
		return getCompletionFor(languageServer, getCursorPosition(textWithCursor)).get().getLeft();
	}

	protected List<String> getCompletionLabels(String textWithCursor, String suffixFileName) throws Exception {
		return getCompletions(textWithCursor, suffixFileName).stream().map(CompletionItem::getLabel).collect(Collectors.toList());
	}

	/**
	 * @return the content of the hover, or null if there is no hover
	 */
	protected String getHoverContent(String textWithCursor, String suffixFileName) throws Exception {
		CamelLanguageServer languageServer = initializeLanguageServer(textWithCursor.replace(CURSOR, ""), suffixFileName);
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI + suffixFileName), getCursorPosition(textWithCursor));
		Hover hover = languageServer.getTextDocumentService().hover(hoverParams).get();
		if (hover == null || hover.getContents() == null) {
			return null;
		}
		if (hover.getContents().isRight()) {
			return hover.getContents().getRight().getValue();
		}
		return hover.getContents().getLeft().stream()
				.map(content -> content.isLeft() ? content.getLeft() : content.getRight().getValue())
				.collect(Collectors.joining("\n"));
	}

	protected static Position getCursorPosition(String textWithCursor) {
		int cursor = textWithCursor.indexOf(CURSOR);
		String textBeforeCursor = textWithCursor.substring(0, cursor);
		int line = (int) textBeforeCursor.chars().filter(c -> c == '\n').count();
		return new Position(line, cursor - textBeforeCursor.lastIndexOf('\n') - 1);
	}
}
