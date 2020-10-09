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

public class CamelKafkaUtil {

	public static final String CAMEL_SINK_URL = "camel.sink.url";
	public static final String CAMEL_SOURCE_URL = "camel.source.url";
	public static final String CONNECTOR_CLASS = "connector.class";
	
	public boolean isCamelURIForKafka(String propertyKey) {
		return CamelKafkaUtil.CAMEL_SINK_URL.equals(propertyKey)
				|| CamelKafkaUtil.CAMEL_SOURCE_URL.equals(propertyKey);
	}
	
	public boolean isInsideACamelUri(String line, int characterPosition) {
		return line.startsWith(CamelKafkaUtil.CAMEL_SOURCE_URL) && CamelKafkaUtil.CAMEL_SOURCE_URL.length() < characterPosition 
				|| line.startsWith(CamelKafkaUtil.CAMEL_SINK_URL) && CamelKafkaUtil.CAMEL_SINK_URL.length() < characterPosition;
	}

	public boolean isConnectorClassForCamelKafkaConnector(String propertyKey) {
		return CONNECTOR_CLASS.equals(propertyKey);
	}

}
