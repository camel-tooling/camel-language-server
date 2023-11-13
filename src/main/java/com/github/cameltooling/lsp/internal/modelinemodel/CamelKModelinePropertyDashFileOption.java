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
package com.github.cameltooling.lsp.internal.modelinemodel;

import java.nio.file.Path;
import java.util.function.Predicate;

import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineOptionNames;

/**
 * For // camel-k: property-file=aFile.properties , it represent the aFile.properties part
 *
 */
public class CamelKModelinePropertyDashFileOption extends CamelKModelineLocalResourceRelatedOption {
	
	public CamelKModelinePropertyDashFileOption(String value, int startPosition, String documentItemUri, int startLine, int endLine) {
		super(value, startPosition, documentItemUri, startLine, endLine);
	}
	
	protected Predicate<Path> getFilter() {
		return path -> path.getFileName().toString().endsWith(".properties");
	}

	@Override
	protected String getPropertyName() {
		return CamelKModelineOptionNames.OPTION_NAME_PROPERTY_FILE;
	}
}
