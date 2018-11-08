/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.log4j.Logger;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.Test;

import com.github.cameltooling.lsp.internal.TestLogAppender;

public class ParserXMLFileHelperTest {

	@Test
	public void testGetCamelComponentUri() throws Exception {
		final TestLogAppender appender = new TestLogAppender();
		final Logger logger = Logger.getRootLogger();
		logger.addAppender(appender);
		new ParserXMLFileHelper().getCamelComponentUri("uri=!!", 2);
		assertThat(appender.getLog().get(0).getMessage()).isEqualTo("Encountered an unsupported URI closure char !");
	}
	
	public void testGetRouteNodesWithNamespacePrefix() throws Exception {
		String camel =
				"<camel:camelContext id=\"camel\" xmlns:camel=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <camel:route id=\"a route\">\r\n" + 
				"      <camel:from uri=\"direct:cafe\"/>\r\n" + 
				"      <camel:split>\r\n" + 
				"        <camel:method bean=\"orderSplitter\"/>\r\n" + 
				"        <camel:to uri=\"direct:drink\"/>\r\n" + 
				"      </camel:split>\r\n" + 
				"    </camel:route>\r\n" + 
				"\r\n" + 
				"    <camel:route id=\"another Route\">\r\n" + 
				"      <camel:from uri=\"direct:drink\"/>\r\n" + 
				"      <camel:recipientList>\r\n" + 
				"        <camel:method bean=\"drinkRouter\"/>\r\n" + 
				"      </camel:recipientList>\r\n" + 
				"    </camel:route>\n"
				+ "</camel:camelContext>\n";
		TextDocumentItem textDocumentItem = new TextDocumentItem(null, null, 0, camel);
		assertThat(new ParserXMLFileHelper().getRouteNodes(textDocumentItem).getLength()).isEqualTo(2);
	}
	
	@Test
	public void testGetRouteNodes() throws Exception {
		String camel =
				"<camelContext id=\"camel\" xmlns=\"http://camel.apache.org/schema/spring\">\r\n" + 
				"\r\n" + 
				"    <route id=\"a route\">\r\n" + 
				"      <from uri=\"direct:cafe\"/>\r\n" + 
				"      <split>\r\n" + 
				"        <method bean=\"orderSplitter\"/>\r\n" + 
				"        <to uri=\"direct:drink\"/>\r\n" + 
				"      </split>\r\n" + 
				"    </route>\r\n" + 
				"\r\n" + 
				"    <route id=\"another Route\">\r\n" + 
				"      <from uri=\"direct:drink\"/>\r\n" + 
				"      <recipientList>\r\n" + 
				"        <method bean=\"drinkRouter\"/>\r\n" + 
				"      </recipientList>\r\n" + 
				"    </route>\n"
				+ "</camelContext>\n";
		TextDocumentItem textDocumentItem = new TextDocumentItem(null, null, 0, camel);
		assertThat(new ParserXMLFileHelper().getRouteNodes(textDocumentItem).getLength()).isEqualTo(2);
	}
}
