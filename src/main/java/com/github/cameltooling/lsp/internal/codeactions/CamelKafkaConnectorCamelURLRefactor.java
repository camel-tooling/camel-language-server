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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.CamelTextDocumentService;
import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.parser.CamelKafkaUtil;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelper;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperFactory;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public class CamelKafkaConnectorCamelURLRefactor {

	public static final String CONVERT_TO_LIST_OF_PROPERTIES_NOTATION = "Convert to list of properties notation.";
	
	private CamelTextDocumentService camelTextDocumentService;

	public CamelKafkaConnectorCamelURLRefactor(CamelTextDocumentService camelTextDocumentService) {
		this.camelTextDocumentService = camelTextDocumentService;
	}

	public Collection<Either<Command, CodeAction>> getCodeActions(CodeActionParams params) {
		String uri = params.getTextDocument().getUri();
		if (uri.endsWith(".properties")) {
			TextDocumentItem openedDocument = camelTextDocumentService.getOpenedDocument(uri);
			int startLine = params.getRange().getStart().getLine();
			int endLine = params.getRange().getEnd().getLine();
			if (startLine == endLine) {
				String line = new ParserFileHelperUtil().getLine(openedDocument, startLine);
				int equalIndex = line.indexOf('=');
				if (equalIndex != -1) {
					String propertyKey = line.substring(0, equalIndex);
					if (new CamelKafkaUtil().isCamelURIForKafka(propertyKey)) {
						ParserFileHelper parserFileHelper = new ParserFileHelperFactory().getCorrespondingParserFileHelper(openedDocument, startLine);
						if (parserFileHelper != null) {
							String camelComponentUri = parserFileHelper.getCamelComponentUri(openedDocument, new Position(startLine, equalIndex + 1));
							if (camelComponentUri != null) {
								TextEdit textEdit = createTextEdit(openedDocument, startLine, line, equalIndex, parserFileHelper, camelComponentUri);
								return createCodeAction(uri, textEdit);
							}
						}
					}
				}
			}
		}
		return Collections.emptyList();
	}

	private Collection<Either<Command, CodeAction>> createCodeAction(String uri, TextEdit textEdit) {
		List<Either<Command, CodeAction>> codeActions = new ArrayList<>();
		Map<String, List<TextEdit>> changes = new HashMap<>();
		changes.put(uri, Arrays.asList(textEdit));
		CodeAction codeAction = new CodeAction(CONVERT_TO_LIST_OF_PROPERTIES_NOTATION);
		codeAction.setKind(CodeActionKind.Refactor);
		codeAction.setEdit(new WorkspaceEdit(changes));
		codeActions.add(Either.forRight(codeAction));
		return codeActions;
	}

	private TextEdit createTextEdit(TextDocumentItem openedDocument, int startLine, String line, int equalIndex, ParserFileHelper parserFileHelper, String camelComponentUri) {
		CamelURIInstance camelURIInstance = parserFileHelper.createCamelURIInstance(openedDocument, new Position(startLine, equalIndex + 1), camelComponentUri);
		String sinkOrSource = line.startsWith(CamelKafkaUtil.CAMEL_SINK_URL) ? CamelKafkaUtil.SINK : CamelKafkaUtil.SOURCE;
		String pathParams = computePathParams(camelURIInstance, sinkOrSource);
		String endpointOptionParams = computeOptions(camelURIInstance, sinkOrSource);
		return new TextEdit(
				new Range(new Position(startLine, 0), new Position(startLine, line.length())),
				concatenate(pathParams, endpointOptionParams));
	}

	private String concatenate(String pathParams, String endpointOptionParams) {
		if(pathParams.isEmpty()) {
			return endpointOptionParams;
		}
		if(endpointOptionParams.isEmpty()) {
			return pathParams;
		}
		return pathParams + "\n" + endpointOptionParams;
	}

	private String computeOptions(CamelURIInstance camelURIInstance, String sinkOrSource) {
		return camelURIInstance.getOptionParams()
				.stream()
				.map(optionParam -> {
					String optionValue = optionParam.getValue().getValueName() != null ? optionParam.getValue().getValueName() : "";
					return CamelKafkaUtil.CAMEL_PREFIX+sinkOrSource+".endpoint."+optionParam.getKey().getKeyName()+"="+optionValue;
				})
				.collect(Collectors.joining("\n"));
	}

	private String computePathParams(CamelURIInstance camelURIInstance, String sinkOrSource) {
		return camelURIInstance.getComponentAndPathUriElementInstance().getPathParams()
				.stream()
				.map(pathParam -> {
					String pathName = pathParam.getName(camelTextDocumentService.getCamelCatalog());
					return CamelKafkaUtil.CAMEL_PREFIX+sinkOrSource+".path."+pathName+"="+ pathParam.getValue();
				})
				.collect(Collectors.joining("\n"));
	}

}
