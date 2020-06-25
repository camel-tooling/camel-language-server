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

import java.util.ArrayList;
import java.util.List;

import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

public class CamelKModeline implements ILineRangeDefineable{
	
	public static final String MODELINE_LIKE_CAMEL_K = "// camel-k:";
	public static final String MODELINE_LIKE_CAMEL_K_YAML = "# camel-k:";
	public static final String MODELINE_LIKE_CAMEL_K_XML = "<!-- camel-k:";
	
	private String fullModeline;
	private List<CamelKModelineOption> options = new ArrayList<>();

	public CamelKModeline(String fullModeline) {
		this.fullModeline = fullModeline;
		String modelineCamelkStart = retrieveModelineCamelKStart(fullModeline);
		if(modelineCamelkStart != null) {
			parseOptions(fullModeline, modelineCamelkStart);
		}
	}

	private void parseOptions(String fullModeline, String modelineCamelkStart) {
		int currentPosition = modelineCamelkStart.length();
		String remainingModeline = fullModeline.substring(currentPosition);
		while(!remainingModeline.isEmpty()) {
			int nextSpaceLikeCharacter = getNextSpaceLikeCharacter(remainingModeline);
			if(nextSpaceLikeCharacter != -1) {
				options.add(new CamelKModelineOption(remainingModeline.substring(1, nextSpaceLikeCharacter), currentPosition + 1));
				remainingModeline = remainingModeline.substring(nextSpaceLikeCharacter);
				currentPosition += nextSpaceLikeCharacter; 
			} else {
				if(!isEnfOfXmlModeline(remainingModeline)) {
					options.add(new CamelKModelineOption(remainingModeline.substring(1), currentPosition + 1));
				}
				remainingModeline = "";
			}
		}
	}

	private boolean isEnfOfXmlModeline(String remainingModeline) {
		return "-->".equals(remainingModeline.substring(1));
	}

	private String retrieveModelineCamelKStart(String fullModeline) {
		if(fullModeline.startsWith(MODELINE_LIKE_CAMEL_K)) {
			return MODELINE_LIKE_CAMEL_K;
		} else if(fullModeline.startsWith(MODELINE_LIKE_CAMEL_K_YAML)) {
			return MODELINE_LIKE_CAMEL_K_YAML;
		} else if(fullModeline.startsWith(MODELINE_LIKE_CAMEL_K_XML)) {
			return MODELINE_LIKE_CAMEL_K_XML;
		}
		return null;
	}

	private int getNextSpaceLikeCharacter(String remainingModeline) {
		int nextSpace = remainingModeline.indexOf(' ', 1);
		int nextTab = remainingModeline.indexOf('\t', 1);
		if(nextSpace == -1) {
			return nextTab;
		} else if(nextTab == -1){
			return nextSpace;
		} else {
			return Math.min(nextSpace, nextTab);
		}
	}

	@Override
	public int getLine() {
		return 0;
	}

	@Override
	public int getStartPositionInLine() {
		return 0;
	}

	@Override
	public int getEndPositionInLine() {
		return fullModeline.length();
	}

	public List<CamelKModelineOption> getOptions() {
		return options;
	}
}
