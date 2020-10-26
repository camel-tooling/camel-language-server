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
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.CamelTextDocumentService;

public class CodeActionProcessor {

	private CamelTextDocumentService camelTextDocumentService;

	public CodeActionProcessor(CamelTextDocumentService camelTextDocumentService) {
		this.camelTextDocumentService = camelTextDocumentService;
	}

	public CompletableFuture<List<Either<Command, CodeAction>>> getCodeActions(CodeActionParams params) {
		CodeActionContext context = params.getContext();
		if (context != null) {
			List<Either<Command, CodeAction>> codeActions = new ArrayList<>();
			List<String> codeActionsType = context.getOnly();
			if (codeActionsType == null) {
				codeActions.addAll(computeQuickfixes(params));
				codeActions.addAll(computeConvertCamelKafkaConnectorUrl(params));
			} else {
				if(codeActionsType.contains(CodeActionKind.QuickFix)) {
					codeActions.addAll(computeQuickfixes(params));
				}
				if(codeActionsType.contains(CodeActionKind.Refactor)) {
					codeActions.addAll(computeConvertCamelKafkaConnectorUrl(params));
				}
			}
			return CompletableFuture.supplyAsync(() -> codeActions);
		} else {
			return CompletableFuture.completedFuture(Collections.emptyList());
		}
	}
	
	private Collection<? extends Either<Command, CodeAction>> computeConvertCamelKafkaConnectorUrl(CodeActionParams params) {
		return new ConvertCamelKafkaConnectorURLToPropertiesRefactorAction(camelTextDocumentService).getCodeActions(params);
	}

	private List<Either<Command, CodeAction>> computeQuickfixes(CodeActionParams params) {
		List<Either<Command, CodeAction>> allQuickfixes = new ArrayList<>();
		allQuickfixes.addAll(new UnknownPropertyQuickfix(camelTextDocumentService).apply(params));
		allQuickfixes.addAll(new InvalidEnumQuickfix(camelTextDocumentService).apply(params));
		return allQuickfixes;
	}

}
