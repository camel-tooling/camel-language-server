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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.junit.jupiter.api.Test;

class PlaceholderErrorsTest {

	private final CamelCatalog catalog = new DefaultCamelCatalog(true);

	@Test
	void testPlaceholderApiNameAndParametersOfItsMethodsAreIgnored() {
		PlaceholderErrors errors = errorsOf("fhir://{{apiName}}/read?serverUrl=http://localhost&resourceClass=Patient");

		assertThat(errors.isInvalidEnum("apiName")).isTrue();
		assertThat(errors.isUnknownOption("resourceClass")).isTrue();
		assertThat(errors.getNumberOfErrors()).isEqualTo(2);
	}

	@Test
	void testOptionsOfNoMethodAreStillUnknown() {
		PlaceholderErrors errors = errorsOf("fhir://{{apiName}}/read?serverUrl=http://localhost&unknownX=1");

		assertThat(errors.isUnknownOption("unknownX")).isFalse();
	}

	@Test
	void testParametersOfTheMethodsOfAnotherApiAreStillUnknown() {
		// the data API is resolved: batchUpdateSpreadsheetRequest is a parameter of the spreadsheets API only
		PlaceholderErrors errors = errorsOf("google-sheets:data/{{operation}}?spreadsheetId=1&batchUpdateSpreadsheetRequest=2");

		assertThat(errors.isInvalidEnum("methodName")).isTrue();
		assertThat(errors.isUnknownOption("spreadsheetId")).isTrue();
		assertThat(errors.isUnknownOption("batchUpdateSpreadsheetRequest")).isFalse();
	}

	@Test
	void testInvalidApiNameIsNotIgnored() {
		PlaceholderErrors errors = errorsOf("fhir://reed/resourceById?serverUrl=http://localhost");

		assertThat(errors.isInvalidEnum("apiName")).isFalse();
		assertThat(errors.getNumberOfErrors()).isZero();
	}

	private PlaceholderErrors errorsOf(String uri) {
		return PlaceholderErrors.of(catalog, uri, catalog.validateEndpointProperties(uri, false));
	}
}
