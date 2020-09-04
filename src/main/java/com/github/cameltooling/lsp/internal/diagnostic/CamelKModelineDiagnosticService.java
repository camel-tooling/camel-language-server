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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModeline;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineOption;
import com.github.cameltooling.lsp.internal.modelinemodel.CamelKModelineTraitOption;
import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public class CamelKModelineDiagnosticService {

	public Collection<Diagnostic> compute(String camelText, String documentItemUri) {
		String modelineString = new ParserFileHelperUtil().getLine(camelText, 0);
		CamelKModeline camelKModeline = new CamelKModeline(modelineString, documentItemUri);
		List<CamelKModelineTraitOption> traitOptions = camelKModeline.getOptions().stream()
				.map(CamelKModelineOption::getOptionValue)
				.filter(CamelKModelineTraitOption.class::isInstance)
				.map(CamelKModelineTraitOption.class::cast)
				.collect(Collectors.toList());
		Collection<Diagnostic> diagnostics = new HashSet<>();
		for (CamelKModelineTraitOption traitOption : traitOptions) {
			for (CamelKModelineTraitOption traitOption2 : traitOptions) {
				if(!traitOption.equals(traitOption2)) {
				String definitionName = traitOption.getTraitDefinition().getValueAsString();
				String propertyName = traitOption.getTraitProperty().getValueAsString();
				if(definitionName != null
					&& propertyName != null
					&& definitionName.equals(traitOption2.getTraitDefinition().getValueAsString())
					&& propertyName.equals(traitOption2.getTraitProperty().getValueAsString())) {
						Range range = new Range(new Position(0,traitOption2.getStartPositionInLine()), new Position(0,traitOption2.getEndPositionInLine()));
						diagnostics.add(new Diagnostic(range, "More than one trait defines the same property: " + definitionName + "." + propertyName));
					}
				}
			}
		}
		return diagnostics;
	}

}
