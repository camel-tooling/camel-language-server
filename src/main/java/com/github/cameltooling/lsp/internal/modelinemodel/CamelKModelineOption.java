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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CompletionItem;

import com.github.cameltooling.lsp.internal.completion.modeline.CamelKTraitManager;
import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

public class CamelKModelineOption implements ILineRangeDefineable {
	
	private String optionName;
	private String optionValue;
	private int startCharacter;

	public CamelKModelineOption(String option, int startCharacter) {
		int nameValueIndexSeparator = option.indexOf('=');
		this.optionName = option.substring(0, nameValueIndexSeparator != -1 ? nameValueIndexSeparator : option.length());
		this.optionValue = nameValueIndexSeparator != -1 ? option.substring(nameValueIndexSeparator+1) : null;
		this.startCharacter = startCharacter;
	}
	
	@Override
	public int getLine() {
		return 0;
	}

	@Override
	public int getStartPositionInLine() {
		return startCharacter;
	}

	@Override
	public int getEndPositionInLine() {
		return startCharacter + optionName.length() + (optionValue != null ? optionValue.length() + 1 : 0);
	}

	public String getOptionName() {
		return optionName;
	}

	public String getOptionValue() {
		return optionValue;
	}

	public boolean isInRange(int positionInLine) {
		return getStartPositionInLine() <= positionInLine && getEndPositionInLine() >= positionInLine;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(int position) {
		if("trait".equals(optionName) && getStartPositionInLine() + "trait=".length() == position) {
			return CompletableFuture.completedFuture(CamelKTraitManager.getTraitDefinitionNameCompletionItems());
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

}
