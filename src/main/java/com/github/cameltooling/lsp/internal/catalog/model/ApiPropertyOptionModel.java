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

public class ApiPropertyOptionModel {

	private String name;
	private ApiPropertyMethodOptionModel creator;
	private ApiPropertyMethodOptionModel deleter;
	private ApiPropertyMethodOptionModel fetcher;
	private ApiPropertyMethodOptionModel reader;
	private ApiPropertyMethodOptionModel updater;
	private ApiPropertyMethodOptionModel propertyMethod;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ApiPropertyMethodOptionModel getCreator() {
		return creator;
	}
	public void setCreator(ApiPropertyMethodOptionModel creator) {
		this.creator = creator;
	}
	public ApiPropertyMethodOptionModel getDeleter() {
		return deleter;
	}
	public void setDeleter(ApiPropertyMethodOptionModel deleter) {
		this.deleter = deleter;
	}
	public ApiPropertyMethodOptionModel getFetcher() {
		return fetcher;
	}
	public void setFetcher(ApiPropertyMethodOptionModel fetcher) {
		this.fetcher = fetcher;
	}
	public ApiPropertyMethodOptionModel getReader() {
		return reader;
	}
	public void setReader(ApiPropertyMethodOptionModel reader) {
		this.reader = reader;
	}
	public ApiPropertyMethodOptionModel getUpdater() {
		return updater;
	}
	public void setUpdater(ApiPropertyMethodOptionModel updater) {
		this.updater = updater;
	}
	public void setPropertyMethod(ApiPropertyMethodOptionModel methodDescriptor) {
		this.propertyMethod = methodDescriptor;
	}
	public ApiPropertyMethodOptionModel getPropertyMethod() {
		return propertyMethod;
	}
}
