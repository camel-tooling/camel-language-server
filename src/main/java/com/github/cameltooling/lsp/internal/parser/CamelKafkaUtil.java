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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;

import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;

public class CamelKafkaUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CamelKafkaUtil.class);
	
	public static final String CAMEL_PREFIX = "camel.";
	public static final String SINK = "sink";
	public static final String SOURCE = "source";
	private static final String URL_SUFFIX = ".url";
	public static final String CAMEL_SINK_URL = CAMEL_PREFIX + SINK + URL_SUFFIX;
	public static final String CAMEL_SOURCE_URL = CAMEL_PREFIX + SOURCE + URL_SUFFIX;
	public static final String CONNECTOR_CLASS = "connector.class";
	public static final String KEY_CONVERTER = "key.converter";
	public static final String VALUE_CONVERTER = "value.converter";
	
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

	public boolean isConverterForCamelKafkaConnector(String propertyKey) {
		return KEY_CONVERTER.equals(propertyKey)
				|| VALUE_CONVERTER.equals(propertyKey);
	}
	
	public String findConnectorClass(TextDocumentItem textDocumentItem) {
		Properties properties = new Properties();
		try {
			properties.load(new ByteArrayInputStream(textDocumentItem.getText().getBytes()));
			Object connectorClassValue = properties.get(CONNECTOR_CLASS);
			if (connectorClassValue != null) {
				return connectorClassValue.toString();
			}
		} catch (IOException e) {
			LOGGER.error("Cannot load Properties file to search for 'connector.class' property value.", e);
		}
		return null;
	}

	public Optional<CamelKafkaConnectorModel> findConnectorModel(TextDocumentItem textDocumentItem, CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		String connectorClass = findConnectorClass(textDocumentItem);
		if (connectorClass != null) {
			Collection<CamelKafkaConnectorModel> camelKafkaConnectors = camelKafkaConnectorManager.getCatalog().getConnectorsModel().values();
			return camelKafkaConnectors.stream()
					.filter(iteratorModel -> connectorClass.equals(iteratorModel.getConnectorClass()))
					.findAny();
		}
		return Optional.empty();
	}

}
