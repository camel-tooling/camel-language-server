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
 * For // camel-k: config=file:aFile.xxx , it represent the aFile.xxx part
 *
 */
public class CamelKModelineConfigFileOption extends CamelKModelineLocalResourceRelatedOption {

	protected CamelKModelineConfigFileOption(String value, int startPosition, String documentItemUri, int line) {
		super(value, startPosition, documentItemUri, line);
	}

	@Override
	protected String getPropertyName() {
		return CamelKModelineOptionNames.OPTION_NAME_CONFIG;
	}

	@Override
	protected Predicate<Path> getFilter() {
		return path -> true;
	}

}
