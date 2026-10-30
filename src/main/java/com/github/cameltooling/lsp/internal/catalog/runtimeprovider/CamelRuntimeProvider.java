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
package com.github.cameltooling.lsp.internal.catalog.runtimeprovider;

import org.apache.camel.catalog.DefaultRuntimeProvider;
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.quarkus.QuarkusRuntimeProvider;
import org.apache.camel.springboot.catalog.SpringBootRuntimeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum CamelRuntimeProvider {
	DEFAULT("Camel", null, null, null),
	SPRINGBOOT("Camel Spring Boot", "org.apache.camel.springboot", "camel-catalog-provider-springboot", "camel-spring-boot"),
	QUARKUS("Camel Quarkus", "org.apache.camel.quarkus", "camel-quarkus-catalog", "camel-quarkus-core");

	private static final Logger LOGGER = LoggerFactory.getLogger(CamelRuntimeProvider.class);

	private final String title;
	private final String catalogGroupId;
	private final String catalogArtifactId;
	private final String configurationArtifactId;

	CamelRuntimeProvider(String title, String catalogGroupId, String catalogArtifactId, String configurationArtifactId) {
		this.title = title;
		this.catalogGroupId = catalogGroupId;
		this.catalogArtifactId = catalogArtifactId;
		this.configurationArtifactId = configurationArtifactId;
	}

	public String getTitle() {
		return title;
	}

	/**
	 * @return the group id of the Maven artifact providing the catalog of a version of the runtime, or null when the runtime has no own catalog
	 */
	public String getCatalogGroupId() {
		return catalogGroupId;
	}

	/**
	 * @return the id of the Maven artifact providing the catalog of a version of the runtime, or null when the runtime has no own catalog
	 */
	public String getCatalogArtifactId() {
		return catalogArtifactId;
	}

	/**
	 * @return the id of the Maven artifact, in the group of the catalog, providing the metadata of the configuration properties
	 * that the runtime adds to the ones of Camel Main, or null when the runtime adds none
	 */
	public String getConfigurationArtifactId() {
		return configurationArtifactId;
	}

	public static RuntimeProvider getProvider(String runtimeProvider){
		try {
			switch (CamelRuntimeProvider.valueOf(runtimeProvider)) {
			case DEFAULT:
				return new DefaultRuntimeProvider();
			case SPRINGBOOT:
				return new SpringBootRuntimeProvider();
			case QUARKUS:
				return new QuarkusRuntimeProvider();
			default:
				LOGGER.warn("Unsupported Runtime Provider: {}", runtimeProvider);
				return null;
			}
		} catch (IllegalArgumentException ex) {
			LOGGER.warn("Unknown Runtime Provider: {}", runtimeProvider);
			return null;
		}
	}
}
