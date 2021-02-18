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
package com.github.cameltooling.lsp.internal.documentsymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import org.apache.camel.parser.model.CamelEndpointDetails;
import org.apache.camel.parser.model.CamelNodeDetails;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public abstract class AbstractDocumentSymbolProcessor {

	protected TextDocumentItem textDocumentItem;

	protected AbstractDocumentSymbolProcessor(TextDocumentItem textDocumentItem) {
		this.textDocumentItem = textDocumentItem;
	}

	protected List<Either<SymbolInformation, DocumentSymbol>> createSymbolInformations(List<CamelNodeDetails> camelNodes, List<CamelEndpointDetails> endpoints) {
		List<Either<SymbolInformation, DocumentSymbol>> symbolInformations = new ArrayList<>();
		if (camelNodes != null) {
			for (CamelNodeDetails camelNodeDetails : camelNodes) {
				Range range = computeRange(camelNodeDetails);
				Optional<String> componentName = endpoints.stream()
						.filter(ced -> Integer.valueOf(ced.getLineNumber()) - 1 == range.getStart().getLine())
						.map(CamelEndpointDetails::getEndpointUri)
						.map(this::shortEndpoint)
						.findFirst();
				symbolInformations.add(createSymbolInformation(camelNodeDetails, range, componentName));
				symbolInformations.addAll(createSymbolInformations(camelNodeDetails.getOutputs(), endpoints));
			}
		}
		return symbolInformations;
	}

	private Either<SymbolInformation, DocumentSymbol> createSymbolInformation(CamelNodeDetails camelNodeDetails, Range range, Optional<String> componentPath) {
		String nodeDetailsName = camelNodeDetails.getName();
		return Either.forLeft(
				new SymbolInformation(
						componentPath.isPresent() ? nodeDetailsName + " " + componentPath.get() : nodeDetailsName,
						SymbolKind.Field,
						new Location(textDocumentItem.getUri(), range)));
	}

	private Range computeRange(CamelNodeDetails camelNodeDetails) {
		int endLine = retrieveEndline(camelNodeDetails);
		Position startPosition = new Position(Integer.valueOf(camelNodeDetails.getLineNumber()) - 1, 0);
		Position endPosition = new Position(endLine, new ParserFileHelperUtil().getLine(textDocumentItem, endLine).length());
		return new Range(startPosition, endPosition);
	}

	private int retrieveEndline(CamelNodeDetails camelNodeDetails) {
		OptionalInt endLineComputedFromChildren = retrieveAllChildrenOutputs(camelNodeDetails)
				.mapToInt(output -> output.getLineNumberEnd() != null ? Integer.valueOf(output.getLineNumberEnd()) - 1 : 0)
				.max();
		return endLineComputedFromChildren.orElse(Integer.valueOf(camelNodeDetails.getLineNumberEnd()) - 1);
	}

	private Stream<CamelNodeDetails> retrieveAllChildrenOutputs(CamelNodeDetails camelNodeDetails) {
		List<CamelNodeDetails> children = camelNodeDetails.getOutputs();
		if (children != null) {
			return Stream.concat(Stream.of(camelNodeDetails), children.stream().flatMap(this::retrieveAllChildrenOutputs));
		} else {
			return Stream.of(camelNodeDetails);
		}
	}

	private String shortEndpoint(String uri) {
	    int pos = uri.indexOf('?');
	    return pos != -1 ? uri.substring(0, pos) : uri;
	}

}
