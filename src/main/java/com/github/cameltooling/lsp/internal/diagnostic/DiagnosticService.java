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
package com.github.cameltooling.lsp.internal.diagnostic;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.catalog.CamelCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.cameltooling.lsp.internal.catalog.diagnostic.CamelDiagnosticMessage;
import com.github.cameltooling.lsp.internal.catalog.util.CamelKafkaConnectorCatalogManager;

public abstract class DiagnosticService {
	
	public static final String APACHE_CAMEL_VALIDATION = "Apache Camel validation";
	protected static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticService.class);
	public static final String ERROR_CODE_UNKNOWN_PROPERTIES = "camel.diagnostic.unknown.properties";
	public static final String ERROR_CODE_INVALID_ENUM = "camel.diagnostic.invalid.enum";
	
	protected CompletableFuture<CamelCatalog> camelCatalog;
	protected CamelKafkaConnectorCatalogManager camelKafkaConnectorCatalogManager;

	protected DiagnosticService(CompletableFuture<CamelCatalog> camelCatalog, CamelKafkaConnectorCatalogManager camelKafkaConnectorCatalogManager) {
		this.camelCatalog = camelCatalog;
		this.camelKafkaConnectorCatalogManager = camelKafkaConnectorCatalogManager;
	}

	protected void logExceptionValidatingDocument(String docUri, Exception e) {
		LOGGER.warn("Error while trying to validate the document {}", docUri, e);
	}
	
	protected void computeErrorMessage(StringBuilder sb, String syntaxError) {
		if(syntaxError != null) {
			sb.append(syntaxError).append("\n");
		}
	}

	protected void computeErrorMessage(StringBuilder sb, Map<String, String> mapEntryErrors, CamelDiagnosticMessage<Entry<String, String>> errorMsgComputer) {
		if (mapEntryErrors != null) {
			for (Map.Entry<String, String> invalid : mapEntryErrors.entrySet()) {
				sb.append(errorMsgComputer.getErrorMessage(invalid)).append("\n");
			}
		}
	}
	
	protected void computeErrorMessage(StringBuilder sb, Set<String> setOfErrors, CamelDiagnosticMessage<Set<String>> errorMsgComputer) {
		if(setOfErrors != null && !setOfErrors.isEmpty()) {
			sb.append(errorMsgComputer.getErrorMessage(setOfErrors)).append("\n");
		}
	}
}
