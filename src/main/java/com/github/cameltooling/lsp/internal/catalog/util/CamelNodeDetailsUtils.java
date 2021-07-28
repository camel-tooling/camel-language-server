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
package com.github.cameltooling.lsp.internal.catalog.util;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;

import org.apache.camel.parser.model.CamelNodeDetails;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.parser.ParserFileHelperUtil;

public class CamelNodeDetailsUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelNodeDetailsUtils.class);

	public Range computeRange(CamelNodeDetails camelNodeDetails, TextDocumentItem textDocumentItem) {
		int endLine = retrieveEndline(camelNodeDetails);
		Position startPosition = new Position(Integer.valueOf(camelNodeDetails.getLineNumber()) - 1, 0);
		Position endPosition = new Position(endLine, new ParserFileHelperUtil().getLine(textDocumentItem, endLine).length());
		return new Range(startPosition, endPosition);
	}

	private int retrieveEndline(CamelNodeDetails camelNodeDetails) {
		OptionalInt endLineComputedFromChildren = retrieveAllChildrenOutputs(camelNodeDetails)
				.mapToInt(output -> {
					String lineNumberEndAsString = output.getLineNumberEnd();
					if(lineNumberEndAsString != null) {
						try {
							return Integer.valueOf(lineNumberEndAsString) - 1;							
						} catch(NumberFormatException ex) {
							LOGGER.warn("The parsing of the file " + camelNodeDetails.getFileName()
									+ " returned an invalid line number end "+lineNumberEndAsString
									+ " for node "+camelNodeDetails.getName(), ex);
							return 0;
						}
					} else {
						return 0;
					}
				})
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
	
}
