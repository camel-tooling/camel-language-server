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
package com.github.cameltooling.lsp.internal.completion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.util.ModelHelper;

public class PomCompletionProcessor {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PomCompletionProcessor.class);

	private TextDocumentItem textDocumentItem;
	private CompletableFuture<CamelCatalog> camelCatalog;

	public PomCompletionProcessor(TextDocumentItem textDocumentItem, CompletableFuture<CamelCatalog> camelCatalog) {
		this.textDocumentItem = textDocumentItem;
		this.camelCatalog = camelCatalog;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(Position position) {
		List<CompletionItem> completions = new ArrayList<>();
		String text = textDocumentItem.getText();
		String[] lines = text.split("\\R");
		if (isInsideTag(position, lines, "profiles")) {
			completions.add(createCamelQuarkusDebugProfileCompletionItem());
		}
		
		if (isInsideTag(position, lines, "dependencies")) {
			try {
				completions.addAll(createCamelDependenciesCompletionItems());
			} catch (Exception e) {
				LOGGER.warn("Error while computing Camel dependencies for Maven pom", e);
			}
		}
		
		return CompletableFuture.completedFuture(completions);
	}

	private List<CompletionItem> createCamelDependenciesCompletionItems() throws InterruptedException, ExecutionException {
		List<CompletionItem> completions = new ArrayList<>();
		List<String> componentNames = camelCatalog.get().findComponentNames();
		for (String componentName : componentNames) {
			ComponentModel componentModel = ModelHelper.generateComponentModel(camelCatalog.get().componentJSonSchema(componentName), false);
			CompletionItem completionItem = new CompletionItem("Camel dependency for component " + componentModel.getTitle());
			completionItem.setInsertText(
					"<dependency>\n" +
					"\t<groupId>"+componentModel.getGroupId()+"</groupId>\n" +
					"\t<artifactId>"+componentModel.getArtifactId()+"</artifactId>\n" +
					"</dependency>\n");
			CompletionResolverUtils.applyDeprecation(completionItem, componentModel.getDeprecated());
			completions.add(completionItem);
		}
		return completions;
	}

	private CompletionItem createCamelQuarkusDebugProfileCompletionItem() {
		CompletionItem completionQuarkusDebugProfile = new CompletionItem("Camel debug profile for Quarkus");
		completionQuarkusDebugProfile.setInsertText(
				"""
					<profile>
					\t<id>camel.debug</id>
					\t<activation>
					\t\t<property>
					\t\t\t<name>camel.debug</name>
					\t\t\t<value>true</value>
					\t\t</property>
					\t</activation>
					\t<dependencies>
					\t\t<dependency>
					\t\t\t<groupId>org.apache.camel.quarkus</groupId>
					\t\t\t<artifactId>camel-quarkus-debug</artifactId>
					\t\t</dependency>
					\t</dependencies>
					</profile>""");
		completionQuarkusDebugProfile.setDocumentation(
				"""
					Adding a profile to enable Camel Debug.
					Combined with launch configuration and tasks, it allows single-click debug.
					It requires Camel Quarkus 2.14+""");
		return completionQuarkusDebugProfile;
	}

	/**
	 * /!\ This can lead to some false positive but I think this is not really important for now.
	 * 
	 * @param position
	 * @param lines
	 * @param enclosingTag
	 * @return
	 */
	private boolean isInsideTag(Position position, String[] lines, String enclosingTag) {
		boolean hasEnclosingTagBefore = false;
		boolean hasEnclosingTagAfter = false;
		for (int i = position.getLine(); i >= 0 && i < lines.length; i--) {
			if (lines[i].contains("<"+enclosingTag+">")) {
				hasEnclosingTagBefore = true;
				break;
			}
			if (lines[i].contains("</"+enclosingTag+">")) {
				return false;
			}
		}
		
		for (int i = position.getLine(); i < lines.length; i++) {
			if (lines[i].contains("</"+enclosingTag+">")) {
				hasEnclosingTagAfter = true;
				break;
			}
		}
		
		return hasEnclosingTagBefore && hasEnclosingTagAfter;
	}

}
