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
package com.github.cameltooling.lsp.internal.codeactions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.CamelTextDocumentService;
import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineOptionNames;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModeline;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineOption;
import com.github.cameltooling.lsp.internal.modelinemodel.ICamelKModelineOptionValue;
import com.github.cameltooling.lsp.internal.parser.CamelKModelineParser;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public class ConvertCamelKPropertyFileModelineRefactorAction {
	
	public static final String CODE_ACTION_TITLE_CONVERT_PROPERTY_FILE = "Convert deprecated modeline options property-file to property=file: notation";

	private CamelTextDocumentService camelTextDocumentService;

	public ConvertCamelKPropertyFileModelineRefactorAction(CamelTextDocumentService camelTextDocumentService) {
		this.camelTextDocumentService = camelTextDocumentService;
	}

	public Collection<Either<Command, CodeAction>> getCodeActions(CodeActionParams params) {
		String uri = params.getTextDocument().getUri();
		TextDocumentItem openedDocument = camelTextDocumentService.getOpenedDocument(uri);
		int startLine = params.getRange().getStart().getLine();
		int endLine = params.getRange().getEnd().getLine();
		if(startLine == endLine && new CamelKModelineParser().isOnCamelKModeline(startLine, openedDocument)) {
			CamelKModeline camelKModeline = new CamelKModeline(new ParserFileHelperUtil().getLine(openedDocument, startLine), openedDocument, startLine, endLine);
			List<Either<Command, CodeAction>> codeActions = new ArrayList<>();
			Map<String, List<TextEdit>> changes = new HashMap<>();
			List<TextEdit> textEdits = new ArrayList<>();
			for (CamelKModelineOption option : camelKModeline.getOptions()) {
				if (CamelKModelineOptionNames.OPTION_NAME_PROPERTY_FILE.equals(option.getOptionName())) {
					ICamelKModelineOptionValue optionValue = option.getOptionValue();
					String newText = "property=file:" + (optionValue!=null?optionValue.getValueAsString():"");
					textEdits.add(new TextEdit(option.getRange(), newText));
				}
			}
			if(!textEdits.isEmpty()) {
				changes.put(uri, textEdits);
				CodeAction codeAction = new CodeAction(CODE_ACTION_TITLE_CONVERT_PROPERTY_FILE);
				codeAction.setKind(CodeActionKind.Refactor);
				codeAction.setEdit(new WorkspaceEdit(changes));
				codeActions.add(Either.forRight(codeAction));
			}
			return codeActions;
		}
		return Collections.emptyList();
	}

}
