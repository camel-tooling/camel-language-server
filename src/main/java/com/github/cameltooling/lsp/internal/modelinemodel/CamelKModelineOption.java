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

import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

public class CamelKModelineOption implements ILineRangeDefineable {
	
	private String optionName;
	private ICamelKModelineOptionValue optionValue;
	private int startCharacter;

	public CamelKModelineOption(String option, int startCharacter) {
		int nameValueIndexSeparator = option.indexOf('=');
		this.startCharacter = startCharacter;
		this.optionName = option.substring(0, nameValueIndexSeparator != -1 ? nameValueIndexSeparator : option.length());
		this.optionValue = createOptionValue(option, nameValueIndexSeparator);
	}

	private ICamelKModelineOptionValue createOptionValue(String option, int nameValueIndexSeparator){
		if(nameValueIndexSeparator != -1) {
			String value = option.substring(nameValueIndexSeparator+1);
			int startPosition = getStartPositionInLine() + optionName.length() + 1;
			if("trait".equals(optionName)) {
				return new CamelKModelineTraitOption(value, startPosition);
			} else {
				return new GenericCamelKModelineOptionValue(value, startPosition);
			}
		} else {
			return null;
		}
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
		if(optionValue != null) {
			return optionValue.getEndPositionInLine();
		} else {
			return startCharacter + optionName.length();
		}
	}

	public String getOptionName() {
		return optionName;
	}

	public ICamelKModelineOptionValue getOptionValue() {
		return optionValue;
	}

	public boolean isInRange(int positionInLine) {
		return getStartPositionInLine() <= positionInLine && getEndPositionInLine() >= positionInLine;
	}

	public CompletableFuture<List<CompletionItem>> getCompletions(int position) {
		if(optionValue != null && optionValue.isInRange(position)) {
			return optionValue.getCompletions(position);
		}
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

}
