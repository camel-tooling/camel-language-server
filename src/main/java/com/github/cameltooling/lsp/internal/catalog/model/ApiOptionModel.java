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
package com.github.cameltooling.lsp.internal.catalog.model;

import java.util.List;

public class ApiOptionModel {
	
	private String name;
	private boolean consumerOnly;
	private boolean producerOnly;
	private List<String> aliases;
	private ApiOptionMethodsModel apiOptionsMethodsModel;
	
	public boolean isConsumerOnly() {
		return consumerOnly;
	}
	public void setConsumerOnly(boolean consumerOnly) {
		this.consumerOnly = consumerOnly;
	}
	public boolean isProducerOnly() {
		return producerOnly;
	}
	public void setProducerOnly(boolean producerOnly) {
		this.producerOnly = producerOnly;
	}
	public List<String> getAliases() {
		return aliases;
	}
	public void setAliases(List<String> aliases) {
		this.aliases = aliases;
	}
	public ApiOptionMethodsModel getApiOptionsMethodsModel() {
		return apiOptionsMethodsModel;
	}
	public void setApiOptionsMethodsModel(ApiOptionMethodsModel apiOptionsMethodsModel) {
		this.apiOptionsMethodsModel = apiOptionsMethodsModel;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
