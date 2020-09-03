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
package com.github.cameltooling.lsp.internal;

public class TestExtraComponentUtil {
	
	public static final String DEFAULT_COMPONENT = "{\n" + 
			" \"component\": {\n" + 
			"    \"kind\": \"component\",\n" + 
			"    \"scheme\": \"acomponent\",\n" + 
			"    \"syntax\": \"acomponent:withsyntax\",\n" + 
			"    \"title\": \"A Component\",\n" + 
			"    \"description\": \"Description of my component.\",\n" + 
			"    \"label\": \"\",\n" + 
			"    \"deprecated\": true,\n" + 
			"    \"deprecationNote\": \"\",\n" + 
			"    \"async\": false,\n" + 
			"    \"consumerOnly\": true,\n" + 
			"    \"producerOnly\": false,\n" + 
			"    \"lenientProperties\": false,\n" + 
			"    \"javaType\": \"org.test.AComponent\",\n" + 
			"    \"firstVersion\": \"1.0.0\",\n" + 
			"    \"groupId\": \"org.test\",\n" + 
			"    \"artifactId\": \"camel-acomponent\",\n" + 
			"    \"version\": \"3.0.0\"\n" + 
			"  },\n" + 
			"  \"componentProperties\": {\n" + 
			"\"aComponentProperty\": { \"kind\": \"parameter\", \"displayName\": \"A Component property \", \"group\": \"common\", \"required\": false, \"type\": \"string\", \"javaType\": \"java.lang.String\", \"deprecated\": false, \"secret\": false, \"defaultValue\": \"aDefaultValue\", \"configurationClass\": \"org.apache.camel.component.knative.KnativeConfiguration\", \"configurationField\": \"configuration\", \"description\": \"A parameter description\" },\n" +
			"\"aSecondComponentProperty\": { \"kind\": \"parameter\", \"displayName\": \"A Second Component property \", \"group\": \"common\", \"required\": false, \"type\": \"string\", \"javaType\": \"java.lang.String\", \"deprecated\": false, \"secret\": false, \"defaultValue\": \"aDefaultValue\", \"configurationClass\": \"org.apache.camel.component.knative.KnativeConfiguration\", \"configurationField\": \"configuration\", \"description\": \"A second parameter description\" }\n" +
			"  },\n" + 
			"  \"properties\": {\n" +
			"  }\n" + 
			"}";
}
