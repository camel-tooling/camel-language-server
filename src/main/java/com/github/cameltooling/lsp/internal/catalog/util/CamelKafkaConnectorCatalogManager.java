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
package com.github.cameltooling.lsp.internal.catalog.util;

import java.util.Optional;

import org.apache.camel.kafkaconnector.catalog.CamelKafkaConnectorCatalog;
import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.apache.kafka.common.config.ConfigDef;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.parser.CamelKafkaUtil;

public class CamelKafkaConnectorCatalogManager {
	
	private static final String CAMEL_KAFKA_CONNECTOR_TYPE_SOURCE = "source";
	private CamelKafkaConnectorCatalog catalog = new CamelKafkaConnectorCatalog();
		
	public CamelKafkaConnectorCatalog getCatalog() {
		return catalog;
	}
	
	public Optional<CamelKafkaConnectorModel> findConnectorModel(String connectorClass) {
		return catalog.getConnectorsModel()
				.values()
				.stream()
				.filter(connector -> connectorClass != null && connectorClass.equals(connector.getConnectorClass()))
				.findAny();
	}

	public ConfigDef retrieveBasicPropertiesConfigDef(TextDocumentItem textDocumentItem) {
		ConfigDef basicPropertiesConfigDef = null;
		Optional<CamelKafkaConnectorModel> optional = findConnectorModel(new CamelKafkaUtil().findConnectorClass(textDocumentItem));
		if(optional.isPresent()) {
			CamelKafkaConnectorModel model = optional.get();
			if(CAMEL_KAFKA_CONNECTOR_TYPE_SOURCE.equals(model.getType())) {
				basicPropertiesConfigDef = getCatalog().getBasicConfigurationForSource();
			} else {
				basicPropertiesConfigDef = getCatalog().getBasicConfigurationForSink();
			}
		}
		return basicPropertiesConfigDef;
	}
}
