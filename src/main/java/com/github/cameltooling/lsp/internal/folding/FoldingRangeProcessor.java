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
package com.github.cameltooling.lsp.internal.folding;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.camel.parser.RouteBuilderParser;
import org.apache.camel.parser.model.CamelNodeDetails;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.util.CamelNodeDetailsUtils;

public class FoldingRangeProcessor {
	
	private static final String CHOICE_EIP_NAME = "choice";
	private static final Logger LOGGER = LoggerFactory.getLogger(FoldingRangeProcessor.class);

	public CompletableFuture<List<FoldingRange>> computeFoldingRanges(TextDocumentItem textDocumentItem) {
		String uri = textDocumentItem.getUri();
		if (uri.endsWith(".java")) {
			try {
				JavaType<?> parsedJavaFile = Roaster.parse(textDocumentItem.getText());
				if (parsedJavaFile instanceof JavaClassSource) {
					JavaClassSource clazz = (JavaClassSource) parsedJavaFile;
					String absolutePathOfCamelFile = new File(URI.create(textDocumentItem.getUri())).getAbsolutePath();
					List<CamelNodeDetails> camelNodes = RouteBuilderParser.parseRouteBuilderTree(clazz, "", absolutePathOfCamelFile, true);
					List<FoldingRange> foldingRanges = computeRouteFoldingRanges(textDocumentItem, camelNodes);
					foldingRanges.addAll(computeChoiceFoldingRanges(textDocumentItem, camelNodes));
					return CompletableFuture.completedFuture(foldingRanges);
				}
			} catch (Exception ex) {
				LOGGER.warn("Error while computing Folding ranges for " + textDocumentItem.getUri(), ex);
			}
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	private Collection<FoldingRange> computeChoiceFoldingRanges(TextDocumentItem textDocumentItem, List<CamelNodeDetails> camelNodes) {
		List<CamelNodeDetails> allNodes = new ArrayList<>();
		for (CamelNodeDetails camelNodeDetails : camelNodes) {
			allNodes.addAll(new CamelNodeDetailsUtils().retrieveAllChildrenOutputs(camelNodeDetails).collect(Collectors.toList()));
		}
		return allNodes.stream()
				.filter(camelNodeDetail -> CHOICE_EIP_NAME.equals(camelNodeDetail.getName()))
				.map(camelNode -> createFoldingRange(textDocumentItem, camelNode))
				.collect(Collectors.toList());
	}

	private List<FoldingRange> computeRouteFoldingRanges(TextDocumentItem textDocumentItem, List<CamelNodeDetails> camelNodes) {
		return camelNodes.stream()
				.map(camelNode -> createFoldingRange(textDocumentItem, camelNode))
				.collect(Collectors.toList());
	}

	private FoldingRange createFoldingRange(TextDocumentItem textDocumentItem, CamelNodeDetails camelNode) {
		Range range = new CamelNodeDetailsUtils().computeRange(camelNode, textDocumentItem);
		return new FoldingRange(range.getStart().getLine(), range.getEnd().getLine());
	}

}
