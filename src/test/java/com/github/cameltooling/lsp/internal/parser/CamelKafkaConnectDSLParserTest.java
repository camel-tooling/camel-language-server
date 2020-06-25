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
package com.github.cameltooling.lsp.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CamelKafkaConnectDSLParserTest {

	@Test
	void testRetrieveCamelComponentURIForCamelSource() throws Exception {
		String lineTotest = "camel.source.url=aws-s3://bucket?autocloseBody=false";
		String camelComponentUri = new CamelKafkaConnectDSLParser().getCamelComponentUri(lineTotest, 17);
		assertThat(camelComponentUri).isEqualTo("aws-s3://bucket?autocloseBody=false");
	}
	
	@Test
	void testRetrieveCamelComponentURIForCamelSink() throws Exception {
		String lineTotest = "camel.sink.url=aws-s3://bucket?autocloseBody=false";
		String camelComponentUri = new CamelKafkaConnectDSLParser().getCamelComponentUri(lineTotest, 17);
		assertThat(camelComponentUri).isEqualTo("aws-s3://bucket?autocloseBody=false");
	}
	
	@Test
	void testRetrieveCamelComponentURIReturnNullWhenOnPropertyPosition() throws Exception {
		String lineTotest = "camel.sink.url=aws-s3://bucket?autocloseBody=false";
		String camelComponentUri = new CamelKafkaConnectDSLParser().getCamelComponentUri(lineTotest, 14);
		assertThat(camelComponentUri).isNull();
	}
	
	@Test
	void testRetrieveCamelComponentURIReturnNullWhenNotSupportedProperty() throws Exception {
		String lineTotest = "camel.unknowproperty.url=aws-s3://bucket?autocloseBody=false";
		String camelComponentUri = new CamelKafkaConnectDSLParser().getCamelComponentUri(lineTotest, 50);
		assertThat(camelComponentUri).isNull();
	}
	
}
