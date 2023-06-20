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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;

public class PomCompletionProcessor {

	public CompletableFuture<List<CompletionItem>> getCompletions() {
		CompletionItem completionQuarkusDebugProfile = new CompletionItem("Camel debug profile for Quarkus");
		completionQuarkusDebugProfile.setInsertText(
				  "<profile>\n"
				+ "    <id>camel.debug</id>\n"
				+ "    <activation>\n"
				+ "        <property>\n"
				+ "            <name>camel.debug</name>\n"
				+ "            <value>true</value>\n"
				+ "        </property>\n"
				+ "    </activation>\n"
				+ "    <dependencies>\n"
				+ "        <dependency>\n"
				+ "            <groupId>org.apache.camel.quarkus</groupId>\n"
				+ "            <artifactId>camel-quarkus-debug</artifactId>\n"
				+ "        </dependency>\n"
				+ "    </dependencies>\n"
				+ "</profile>");
		completionQuarkusDebugProfile.setDocumentation(
				"Adding a profile to enable Camel Debug.\n"
				+ "Combined with launch configuration and tasks, it allows single-click debug.\n"
				+ "It requires Camel Quarkus 2.14+");
		return CompletableFuture.completedFuture(Collections.singletonList(completionQuarkusDebugProfile));
	}

}
