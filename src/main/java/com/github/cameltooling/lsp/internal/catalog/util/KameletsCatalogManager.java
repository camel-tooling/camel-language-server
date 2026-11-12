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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.camel.kamelets.catalog.KameletsCatalog;
import org.apache.camel.kamelets.catalog.model.KameletLabelNames;
import org.apache.camel.v1.Kamelet;
import org.apache.camel.v1.kameletspec.Definition;
import org.eclipse.lsp4j.TextDocumentItem;

/**
 * The Kamelets known by the Language Server: the local Kamelets of the application, which win, then the Kamelets
 * catalog. Use {@link #forDocument(String)} to see the local Kamelets of a document.
 */
public class KameletsCatalogManager {

	private final KameletsCatalog kameletsCatalog;
	private final LocalKamelets localKamelets;
	private final String documentUri;

	public KameletsCatalogManager() {
		this(Collections::emptyList);
	}

	public KameletsCatalogManager(Supplier<Collection<TextDocumentItem>> openedDocuments) {
		this(new KameletsCatalog(), new LocalKamelets(openedDocuments), null);
	}

	private KameletsCatalogManager(KameletsCatalog kameletsCatalog, LocalKamelets localKamelets, String documentUri) {
		this.kameletsCatalog = kameletsCatalog;
		this.localKamelets = localKamelets;
		this.documentUri = documentUri;
	}

	/**
	 * @return the Kamelets seen from the given document, whose directory resolves the relative local Kamelet directories
	 */
	public KameletsCatalogManager forDocument(String documentUri) {
		return new KameletsCatalogManager(kameletsCatalog, localKamelets, documentUri);
	}

	/**
	 * @return the Kamelets catalog, without the local Kamelets
	 */
	public KameletsCatalog getCatalog() {
		return kameletsCatalog;
	}

	public void setLocalKameletDirectories(List<String> directories) {
		localKamelets.setDirectories(directories);
	}

	/**
	 * @return the Kamelets by name: the local Kamelets, then the ones of the catalog
	 */
	public Map<String, Kamelet> getKamelets() {
		Map<String, Kamelet> local = localKamelets.getKamelets(documentUri);
		if (local.isEmpty()) {
			return kameletsCatalog.getKamelets();
		}
		Map<String, Kamelet> kamelets = new LinkedHashMap<>(local);
		kameletsCatalog.getKamelets().forEach(kamelets::putIfAbsent);
		return kamelets;
	}

	public Kamelet getKamelet(String name) {
		Kamelet local = localKamelets.getKamelets(documentUri).get(name);
		return local != null ? local : kameletsCatalog.getKamelets().get(name);
	}

	public Definition getKameletDefinition(String name) {
		Kamelet kamelet = getKamelet(name);
		return kamelet != null && kamelet.getSpec() != null ? kamelet.getSpec().getDefinition() : null;
	}

	/**
	 * @param type source, sink or action
	 * @return the Kamelets of the given type by name
	 */
	public Map<String, Kamelet> getKameletsByType(String type) {
		Map<String, Kamelet> kamelets = new LinkedHashMap<>();
		getKamelets().forEach((name, kamelet) -> {
			Map<String, String> labels = kamelet.getMetadata() != null ? kamelet.getMetadata().getLabels() : null;
			String kameletType = labels != null ? labels.get(KameletLabelNames.KAMELET_LABEL_TYPE) : null;
			if (kameletType != null && kameletType.contains(type)) {
				kamelets.put(name, kamelet);
			}
		});
		return kamelets;
	}
}
