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
package com.github.cameltooling.lsp.internal.modelinemodel;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineOptionNames;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public class CamelKModelineOption implements ILineRangeDefineable {
	
	private static final String END_OF_XML_COMMENT = "-->";
	private String optionName;
	private ICamelKModelineOptionValue optionValue;
	private int startCharacter;
	private int line;

	public CamelKModelineOption(String option, int startCharacter, TextDocumentItem documentItem, int line) {
		this.line = line;
		int nameValueIndexSeparator = option.indexOf('=');
		this.startCharacter = startCharacter;
		this.optionName = option.substring(0, nameValueIndexSeparator != -1 ? nameValueIndexSeparator : option.length());
		this.optionValue = createOptionValue(option, nameValueIndexSeparator, documentItem);
	}

	private ICamelKModelineOptionValue createOptionValue(String option, int nameValueIndexSeparator, TextDocumentItem documentItem){
		if(nameValueIndexSeparator != -1) {
			String value = option.substring(nameValueIndexSeparator+1);
			if (isEndOfCommentStuckToEndLine(option, documentItem, value)) {
				value = value.substring(0, value.length() - END_OF_XML_COMMENT.length());
			}
			int startPosition = getStartPositionInLine() + optionName.length() + 1;
			if(CamelKModelineOptionNames.OPTION_NAME_TRAIT.equals(optionName)) {
				return new CamelKModelineTraitOption(value, startPosition, line);
			} else if(CamelKModelineOptionNames.OPTION_NAME_DEPENDENCY.equals(optionName)) {
				return new CamelKModelineDependencyOption(value, startPosition, line);
			} else if(CamelKModelineOptionNames.OPTION_NAME_PROPERTY.equals(optionName)) {
				return new CamelKModelinePropertyOption(value, startPosition, documentItem, line);
			} else if(CamelKModelineOptionNames.OPTION_NAME_PROPERTY_FILE.equals(optionName)) {
				return new CamelKModelinePropertyDashFileOption(value, startPosition, documentItem.getUri(), line);
			} else if(CamelKModelineOptionNames.OPTION_NAME_RESOURCE.equals(optionName)) {
				return new CamelKModelineResourceOption(value, startPosition, documentItem.getUri(), line);
			} else if(CamelKModelineOptionNames.OPTION_NAME_OPEN_API.equals(optionName)) {
				return new CamelKModelineOpenAPIOption(value, startPosition, documentItem.getUri(), line);
			} else if(CamelKModelineOptionNames.OPTION_NAME_CONFIG.equals(optionName)) {
				return new CamelKModelineConfigOption(value, startPosition, line);
			} else {
				return new GenericCamelKModelineOptionValue(value, startPosition, line);
			}
		} else {
			return null;
		}
	}

	private boolean isEndOfCommentStuckToEndLine(String option, TextDocumentItem documentItem, String value) {
		return value.endsWith(END_OF_XML_COMMENT)
				&& documentItem.getUri().endsWith(".xml")
				&& startCharacter + option.length() == new ParserFileHelperUtil().getLine(documentItem, getLine()).length();
	}
	
	@Override
	public int getLine() {
		return line;
	}

	@Override
	public int getStartPositionInLine() {
		return startCharacter;
	}

	@Override
	public int getEndPositionInLine() {
		if(optionValue != null) {
			return optionValue.getEndPositionInLine();
		} else {
			return startCharacter + optionName.length();
		}
	}

	public String getOptionName() {
		return optionName;
	}

	public ICamelKModelineOptionValue getOptionValue() {
		return optionValue;
	}

	public boolean isInRange(int positionInLine) {
		return getStartPositionInLine() <= positionInLine && getEndPositionInLine() >= positionInLine;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(int position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(optionValue != null && optionValue.isInRange(position)) {
			return optionValue.getCompletions(position, camelCatalog);
		} else {
			String filter = optionName.substring(0, position - getStartPositionInLine());
			return CompletableFuture.completedFuture(CamelKModelineOptionNames.getCompletionItems(filter));
		}
	}
	
	public CompletableFuture<Hover> getHover(Position position, CompletableFuture<CamelCatalog> camelCatalog) {
		if(getStartPositionInLine() <= position.getCharacter() && position.getCharacter() <= getStartPositionInLine() + optionName.length()) {
			String description = CamelKModelineOptionNames.getDescription(optionName);
			if(description != null) {
				Hover hover = new Hover();
				hover.setContents(Collections.singletonList((Either.forLeft(description))));
				hover.setRange(new Range(new Position(getLine(), getStartPositionInLine()), new Position(getLine(), getStartPositionInLine() + optionName.length())));
				return CompletableFuture.completedFuture(hover);
			}
		}
		if(optionValue != null && optionValue.isInRange(position.getCharacter())) {
			return optionValue.getHover(position.getCharacter(), camelCatalog);
		}
		return CompletableFuture.completedFuture(null);
	}

}
