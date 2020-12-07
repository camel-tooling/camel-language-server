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
package com.github.cameltooling.lsp.internal.diagnostic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModeline;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineOption;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitOption;
import com.github.cameltooling.lsp.internal.parser.CamelKModelineParser;

public class CamelKModelineDiagnosticService extends DiagnosticService {

	public CamelKModelineDiagnosticService() {
		super(null, null);
	}

	public Collection<Diagnostic> compute(String camelText, TextDocumentItem documentItem) {
		BufferedReader bufReader = new BufferedReader(new StringReader(camelText));
		String line=null;
		int lineNumber = 0;
		Collection<Diagnostic> diagnostics = new HashSet<>();
		CamelKModelineParser camelKModelineParser = new CamelKModelineParser();
		try {
			while((line=bufReader.readLine()) != null){
				if(camelKModelineParser.retrieveModelineCamelKStart(line) != null) {
					diagnostics.addAll(computeDiagnosticForLine(documentItem, line, lineNumber));
				}
				lineNumber++;
			}
		} catch (IOException e) {
			logExceptionValidatingDocument(documentItem.getUri(), e);
		}
		return diagnostics;
	}

	private Collection<Diagnostic> computeDiagnosticForLine(TextDocumentItem documentItem, String line, int lineNumber) {
		Collection<Diagnostic> diagnostics = new HashSet<>();
		CamelKModeline camelKModeline = new CamelKModeline(line, documentItem, lineNumber);
		List<CamelKModelineTraitOption> traitOptions = camelKModeline.getOptions().stream()
				.map(CamelKModelineOption::getOptionValue)
				.filter(CamelKModelineTraitOption.class::isInstance)
				.map(CamelKModelineTraitOption.class::cast)
				.collect(Collectors.toList());
		for (CamelKModelineTraitOption traitOption : traitOptions) {
			for (CamelKModelineTraitOption traitOption2 : traitOptions) {
				if(!traitOption.equals(traitOption2)) {
				String definitionName = traitOption.getTraitDefinition().getValueAsString();
				String propertyName = traitOption.getTraitProperty().getValueAsString();
				if(definitionName != null
					&& propertyName != null
					&& definitionName.equals(traitOption2.getTraitDefinition().getValueAsString())
					&& propertyName.equals(traitOption2.getTraitProperty().getValueAsString())) {
						Range range = new Range(new Position(traitOption2.getLine(), traitOption2.getStartPositionInLine()), new Position(traitOption2.getLine(), traitOption2.getEndPositionInLine()));
						diagnostics.add(new Diagnostic(range, "More than one trait defines the same property: " + definitionName + "." + propertyName));
					}
				}
			}
		}
		return diagnostics;
	}

}
