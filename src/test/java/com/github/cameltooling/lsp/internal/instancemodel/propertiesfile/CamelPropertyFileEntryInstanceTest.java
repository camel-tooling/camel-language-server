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
package com.github.cameltooling.lsp.internal.instancemodel.propertiesfile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.CamelLanguageServer;

class CamelPropertyFileEntryInstanceTest {

	@Test
	void testEmpty() throws Exception {
		CamelPropertyFileEntryInstance cpfei = createModel("");
		assertThat(cpfei.getCamelPropertyFileKeyInstance().getCamelPropertyFileKey()).isEmpty();
	}
	
	@Test
	void testKey() throws Exception {
		CamelPropertyFileEntryInstance cpfei = createModel("camel.akey=avalue");
		assertThat(cpfei.getCamelPropertyFileKeyInstance().getCamelPropertyFileKey()).isEqualTo("camel.akey");
	}
	
	@Test
	void testComponent() throws Exception {
		CamelPropertyFileEntryInstance cpfei = createModel("camel.component.acomponentid.akey=avalue");
		assertThat(cpfei.getCamelPropertyFileKeyInstance().getCamelPropertyFileKey()).isEqualTo("camel.component.acomponentid.akey");
		assertThat(cpfei.getCamelPropertyFileKeyInstance().getCamelComponentPropertyFilekey().getComponentId()).isEqualTo("acomponentid");
		assertThat(cpfei.getCamelPropertyFileKeyInstance().getCamelComponentPropertyFilekey().getComponentProperty()).isEqualTo("akey");
	}
	
	@Test
	void testValue() throws Exception {
		CamelPropertyFileEntryInstance cpfei = createModel("camel.akey=avalue");
		assertThat(cpfei.getCamelPropertyFileValueInstance().getCamelPropertyFileValue()).isEqualTo("avalue");
	}

	private CamelPropertyFileEntryInstance createModel(String lineToTest) {
		return new CamelPropertyFileEntryInstance(CompletableFuture.completedFuture((CamelCatalog)null), lineToTest, 0, createTextDocumentItem(lineToTest));
	}

	private TextDocumentItem createTextDocumentItem(String value) {
		return new TextDocumentItem("uri.properties", CamelLanguageServer.LANGUAGE_ID, 1, value);
	}
	
}
