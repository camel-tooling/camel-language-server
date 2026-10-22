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

import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.AbstractCursorTest;

class CamelXmlEipHoverTest extends AbstractCursorTest {

	private static final String XML = ".xml";

	@Test
	void testHoverOnEipElement() throws Exception {
		assertThat(getHoverContent("""
				<routes xmlns="http://camel.apache.org/schema/spring">
				  <route>
				    <from uri="timer:tick"/>
				    <l§og message="${body}"/>
				  </route>
				</routes>
				""", XML))
			.startsWith("Log");
	}

	@Test
	void testHoverOnEndTag() throws Exception {
		assertThat(getHoverContent("""
				<routes xmlns="http://camel.apache.org/schema/spring">
				  <route>
				    <from uri="timer:tick"/>
				  </rou§te>
				</routes>
				""", XML))
			.startsWith("Route");
	}

	@Test
	void testHoverOnElementWithNamespacePrefix() throws Exception {
		assertThat(getHoverContent("""
				<camel:routes xmlns:camel="http://camel.apache.org/schema/spring">
				  <camel:route>
				    <camel:from uri="timer:tick"/>
				    <camel:l§og message="${body}"/>
				  </camel:route>
				</camel:routes>
				""", XML))
			.startsWith("Log");
	}

	@Test
	void testHoverOnUriKeepsEndpointDocumentation() throws Exception {
		assertThat(getHoverContent("""
				<routes xmlns="http://camel.apache.org/schema/spring">
				  <route>
				    <from uri="ti§mer:tick"/>
				  </route>
				</routes>
				""", XML))
			.contains("Generate messages in specified intervals");
	}

	@Test
	void testNoHoverOnElementOutsideOfCamelNamespace() throws Exception {
		assertThat(getHoverContent("""
				<beans xmlns="http://www.springframework.org/schema/beans">
				  <be§an id="dataSource" class="org.example.DataSource"/>
				  <camelContext xmlns="http://camel.apache.org/schema/spring">
				    <route>
				      <from uri="timer:tick"/>
				      <bean ref="dataSource"/>
				    </route>
				  </camelContext>
				</beans>
				""", XML))
			.isNull();
	}

	@Test
	void testNoHoverInXmlWhichIsNotCamel() throws Exception {
		assertThat(getHoverContent("""
				<project>
				  <build>
				    <fil§ter/>
				  </build>
				</project>
				""", XML))
			.isNull();
	}
}
