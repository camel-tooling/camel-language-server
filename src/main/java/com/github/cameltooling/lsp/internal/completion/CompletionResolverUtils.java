/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.lsp.internal.completion;

import java.util.HashMap;
import java.util.Map;

import com.github.cameltooling.lsp.internal.instancemodel.CamelUriElementInstance;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * @author lheinema
 */
public class CompletionResolverUtils {
	
	public static final String KEY_REPLACE_RANGE_START = "startChar";
	public static final String KEY_REPLACE_RANGE_END = "endChar";
	public static final String KEY_LINE = "lineNo";
	public static final String KEY_REPLACEMENT = "replacement";
	
	private CompletionResolverUtils() {
		// util class
	}
	
	public static Map<String, Object> getCompletionResolverDataForUriInstance(CamelUriElementInstance uriInstance, String replacement) {
		Map<String, Object> map = new HashMap<>();
		
		if (uriInstance != null && uriInstance.getCamelUriInstance().getAbsoluteBounds() != null) {
			if (uriInstance.getDocument() != null) {
				map.put("documentURI", uriInstance.getDocument().getUri());
			}
			map.put(KEY_LINE, Integer.toString(uriInstance.getCamelUriInstance().getAbsoluteBounds().getStart().getLine()));
			map.put(KEY_REPLACE_RANGE_START, Integer.toString(uriInstance.getStartPositionInLine()));
			map.put(KEY_REPLACE_RANGE_END,   Integer.toString(uriInstance.getEndPositionInLine()));
			map.put(KEY_REPLACEMENT, replacement);
		}
		
		return map;
	}
	
	public static <T> T getDataFromJSON(Object object, Class<T> clazz) {
		if(object == null){
			return null;
		}
		if(clazz == null ){
			throw new IllegalArgumentException("Class cannot be null");
		}
		if(object instanceof JsonElement){
			Gson gson = new Gson();
			return gson.fromJson((JsonElement) object, clazz);
		}
		if(clazz.isInstance(object)){
			return clazz.cast(object);
		}
		return null;
	}
}
