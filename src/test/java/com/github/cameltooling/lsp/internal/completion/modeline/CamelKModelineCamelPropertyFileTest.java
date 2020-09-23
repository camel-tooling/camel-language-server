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
package com.github.cameltooling.lsp.internal.completion.modeline;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.google.common.io.Files;

class CamelKModelineCamelPropertyFileTest extends AbstractCamelLanguageServerTest {
	
	@TempDir
	File temporaryDir;
	
	@Test
	void testProvideCompletionForPropertyFile() throws Exception {
		File camelKfile = createFileStructureForTest();
		
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(camelKfile);
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 25), camelKfile.toURI().toString());
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).containsOnly(
				createExpectedCompletionItem("a.properties"),
				createExpectedCompletionItem("myFolder/aSecond.properties"));
	}

	private CompletionItem createExpectedCompletionItem(String text) {
		CompletionItem siblingCompletionItem = new CompletionItem(text);
		siblingCompletionItem.setTextEdit(new TextEdit(new Range(new Position(0, 25), new Position(0, 25)), text));
		return siblingCompletionItem;
	}

	/**
	 * temporaryDir
	 *   | test.camelk.yaml
	 *   | anotherFile.txt
	 *   | a.properties
	 *   | myFolder
	 *   --| aSecond.properties
	 *   | .vscode
	 *   --| shouldbefiltered.properties
	 *   | .settings
	 *   --| shouldbefiltered2.properties
	 *   | .theia
	 *   --| shouldbefiltered3.properties
	 * 
	 * @return The Camel K yaml file created at the root of the temporary directory
	 * @throws IOException
	 */
	private File createFileStructureForTest() throws IOException {
		File camelKfile = new File(temporaryDir, "test.camelk.yaml");
		Files.write("# camel-k: property-file=".getBytes(), camelKfile);
		File aSiblingPropertyFile = new File(temporaryDir, "a.properties");
		aSiblingPropertyFile.createNewFile();
		
		createFolderWithFile("myFolder", "aSecond.properties", temporaryDir);
		createFolderWithFile(".vscode", "shouldbefiltered.properties", temporaryDir);
		createFolderWithFile(".settings", "shouldbefiltered2.properties", temporaryDir);
		createFolderWithFile(".theia", "shouldbefiltered3.properties", temporaryDir);
		
		File anotherFile = new File(temporaryDir, "anotherFile.txt");
		anotherFile.createNewFile();
		return camelKfile;
	}
	
	@Test
	void testNoExceptionWithPartialURI() throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer("# camel-k: property-file=", ".yaml");
		
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 25));
		
		List<CompletionItem> completionItems = completions.get().getLeft();
		assertThat(completionItems).isEmpty();
	}
}
