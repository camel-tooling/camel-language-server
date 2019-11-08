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
package com.github.cameltooling.lsp.internal.settings;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class JSONUtility {

	/**
	 * Converts given JSON objects to given Model objects.
	 *
	 * @throws IllegalArgumentException if clazz is null
	 */
	public <T> T toModel(Object object, Class<T> clazz) {
		return toModel(new Gson(), object, clazz);
	}

	private <T> T toModel(Gson gson, Object object, Class<T> clazz) {
		if (object == null) {
			return null;
		}
		if (clazz == null) {
			throw new IllegalArgumentException("Class can not be null");
		}
		if (object instanceof JsonElement) {
			return gson.fromJson((JsonElement) object, clazz);
		}
		if (clazz.isInstance(object)) {
			return clazz.cast(object);
		}
		if (object instanceof String) {
			return gson.fromJson((String) object, clazz);
		}
		return null;
	}
}
