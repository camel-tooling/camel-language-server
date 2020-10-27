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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.cameltooling.lsp.internal.catalog.model.ApiOptionMethodDescriptorModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ApiPropertyOptionModel;
import com.github.cameltooling.lsp.internal.catalog.model.ComponentModel;
import com.github.cameltooling.lsp.internal.catalog.model.EndpointOptionModel;

class ModelHelperApisTest {
	
	private static final String SIMPLFIED_JSON = "{\n"
			+ "	\"component\": {\n"
			+ "		\"kind\": \"component\",\n"
			+ "		\"name\": \"twilio\",\n"
			+ "		\"title\": \"Twilio\",\n"
			+ "		\"description\": \"Interact with Twilio REST APIs using Twilio Java SDK.\",\n"
			+ "		\"deprecated\": false,\n"
			+ "		\"firstVersion\": \"2.20.0\",\n"
			+ "		\"label\": \"api,messaging,cloud\",\n"
			+ "		\"javaType\": \"org.apache.camel.component.twilio.TwilioComponent\",\n"
			+ "		\"supportLevel\": \"Stable\",\n"
			+ "		\"groupId\": \"org.apache.camel\",\n"
			+ "		\"artifactId\": \"camel-twilio\",\n"
			+ "		\"version\": \"3.7.0-SNAPSHOT\",\n"
			+ "		\"scheme\": \"twilio\",\n"
			+ "		\"extendsScheme\": \"\",\n"
			+ "		\"syntax\": \"twilio:apiName\\/methodName\",\n"
			+ "		\"async\": false,\n"
			+ "		\"api\": true,\n"
			+ "		\"apiSyntax\": \"apiName\\/methodName\",\n"
			+ "		\"consumerOnly\": false,\n"
			+ "		\"producerOnly\": false,\n"
			+ "		\"lenientProperties\": false\n"
			+ "	},\n"
			+ "	\"apis\": {\n"
			+ "		\"account\": {\n"
			+ "			\"consumerOnly\": false,\n"
			+ "			\"producerOnly\": false,\n"
			+ "			\"description\": \"\",\n"
			+ "			\"aliases\": [\n"
			+ "				\"^creator$=create\",\n"
			+ "				\"^deleter$=delete\",\n"
			+ "				\"^fetcher$=fetch\",\n"
			+ "				\"^reader$=read\",\n"
			+ "				\"^updater$=update\"\n"
			+ "			],\n"
			+ "			\"methods\": {\n"
			+ "				\"fetcher\": {\n"
			+ "					\"description\": \"Create a AccountFetcher to execute fetch\",\n"
			+ "					\"signatures\": [\n"
			+ "						\"com.twilio.rest.api.v2010.AccountFetcher fetcher()\",\n"
			+ "						\"com.twilio.rest.api.v2010.AccountFetcher fetcher(String pathSid)\"\n"
			+ "					]\n"
			+ "				},\n"
			+ "				\"updater\": {\n"
			+ "					\"description\": \"Create a AccountUpdater to execute update\",\n"
			+ "					\"signatures\": [\n"
			+ "						\"com.twilio.rest.api.v2010.AccountUpdater updater()\",\n"
			+ "						\"com.twilio.rest.api.v2010.AccountUpdater updater(String pathSid)\"\n"
			+ "					]\n"
			+ "				}\n"
			+ "			}\n"
			+ "		}\n"
			+ "	},\n"
			+ "	\"apiProperties\": {\n"
			+ "		\"account\": {\n"
			+ "			\"methods\": {\n"
			+ "				\"fetcher\": {\n"
			+ "					\"properties\": {\n"
			+ "						\"pathSid\": {\n"
			+ "							\"kind\": \"parameter\",\n"
			+ "							\"displayName\": \"Path Sid\",\n"
			+ "							\"group\": \"common\",\n"
			+ "							\"label\": \"\",\n"
			+ "							\"required\": false,\n"
			+ "							\"type\": \"string\",\n"
			+ "							\"javaType\": \"java.lang.String\",\n"
			+ "							\"deprecated\": false,\n"
			+ "							\"secret\": false,\n"
			+ "							\"description\": \"Fetch by unique Account Sid\",\n"
			+ "							\"optional\": false\n"
			+ "						}\n"
			+ "					}\n"
			+ "				},\n"
			+ "				\"updater\": {\n"
			+ "					\"properties\": {\n"
			+ "						\"pathSid\": {\n"
			+ "							\"kind\": \"parameter\",\n"
			+ "							\"displayName\": \"Path Sid\",\n"
			+ "							\"group\": \"common\",\n"
			+ "							\"label\": \"\",\n"
			+ "							\"required\": false,\n"
			+ "							\"type\": \"string\",\n"
			+ "							\"javaType\": \"java.lang.String\",\n"
			+ "							\"deprecated\": false,\n"
			+ "							\"secret\": false,\n"
			+ "							\"description\": \"Update by unique Account Sid\",\n"
			+ "							\"optional\": false\n"
			+ "						}\n"
			+ "					}\n"
			+ "				}\n"
			+ "			}\n"
			+ "		}\n"
			+ "	}\n"
			+ "}";

	@Test
	void testLoadApis() throws Exception {
		ComponentModel componentModel = ModelHelper.generateComponentModel(SIMPLFIED_JSON, true);
		ApiOptionModel apiOptionModel = componentModel.getApis().get(0);
		assertThat(apiOptionModel.getName()).isEqualTo("account");
		assertThat(apiOptionModel.getAliases()).isNotEmpty();
		assertThat(apiOptionModel.isConsumerOnly()).isFalse();
		assertThat(apiOptionModel.isProducerOnly()).isFalse();
		ApiOptionMethodDescriptorModel fetcher = apiOptionModel.getApiOptionsMethodsModel().getFetcher();
		assertThat(fetcher).isNotNull();
		assertThat(fetcher.getDescription()).isEqualTo("Create a AccountFetcher to execute fetch");
		assertThat(fetcher.getSignatures()).containsExactlyInAnyOrder("com.twilio.rest.api.v2010.AccountFetcher fetcher()", "com.twilio.rest.api.v2010.AccountFetcher fetcher(String pathSid)");
	}
	
	@Test
	void testLoadApiProperties() throws Exception {
		ComponentModel componentModel = ModelHelper.generateComponentModel(SIMPLFIED_JSON, true);
		ApiPropertyOptionModel apiPropertyOptionModel = componentModel.getApiProperties().get(0);
		assertThat(apiPropertyOptionModel.getName()).isEqualTo("account");
		List<EndpointOptionModel> propertiesForFetcher = apiPropertyOptionModel.getFetcher().getProperties();
		assertThat(propertiesForFetcher).hasSize(1);
		EndpointOptionModel pathSidFetcherproperty = propertiesForFetcher.get(0);
		assertThat(pathSidFetcherproperty.getName()).isEqualTo("pathSid");
		assertThat(pathSidFetcherproperty.getGroup()).isEqualTo("common");
		assertThat(pathSidFetcherproperty.getKind()).isEqualTo("parameter");
		assertThat(pathSidFetcherproperty.isRequired()).isFalse();
		assertThat(pathSidFetcherproperty.getDescription()).isEqualTo("Fetch by unique Account Sid");
		assertThat(apiPropertyOptionModel.getUpdater()).isNotNull();
	}
}
