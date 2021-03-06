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

import java.util.List;

import org.apache.camel.kafkaconnector.model.CamelKafkaConnectorModel;
import org.eclipse.lsp4j.TextDocumentItem;

import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;
import com.github.cameltooling.lsp.internal.instancemodel.propertiesfile.CamelPropertyValueInstance;

public class CamelKafkaConverterCompletionProcessor extends AbstractConnectorClassDependentCompletionProcessor {

	public CamelKafkaConverterCompletionProcessor(TextDocumentItem textDocumentItem, CamelPropertyValueInstance camelPropertyValueInstance, CamelKafkaConnectorCatalogManager camelKafkaConnectorManager) {
		super(textDocumentItem, camelPropertyValueInstance, camelKafkaConnectorManager);
	}

	@Override
	protected List<String> retrieveList(CamelKafkaConnectorModel model) {
		return model.getConverters();
	}

}
