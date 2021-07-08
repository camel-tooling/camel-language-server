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
package com.github.cameltooling.lsp.internal.hover;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.completion.CamelCompletionForApisTest;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;

class CamelApiBasedComponentHoverTest extends AbstractCamelLanguageServerTest {
	
	@Test
	void tesOnOption() throws Exception {
		String text = RouteTextBuilder.createXMLSpringRoute("aComponentWithApis:account/fetch?aPropertyFetcher=test");
		CamelLanguageServer languageServer = initializeLanguageServer(text);
		
		HoverParams hoverParams = new HoverParams(new TextDocumentIdentifier(DUMMY_URI+".xml"), new Position(0, 45));
		Hover hover = languageServer.getTextDocumentService().hover(hoverParams).get();
		
		assertThat(hover.getContents().getLeft().get(0).getLeft()).isEqualTo("A Property Fetcher description");
		assertThat(hover.getRange()).isEqualTo(new Range(new Position(0, 44), new Position(0, 60)));
	}
	
	@Override
	protected Map<Object, Object> getInitializationOptions() {
		return createMapSettingsWithComponent(CamelCompletionForApisTest.SIMPLIFIED_JSON);
	}

}
