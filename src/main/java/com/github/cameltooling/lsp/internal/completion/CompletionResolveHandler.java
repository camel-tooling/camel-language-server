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

import java.util.Map;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;

/**
* @author lheinema
*/
public class CompletionResolveHandler {

	public CompletionItem resolve(CompletionItem param) {
		Map data = CompletionResolverUtils.getDataFromJSON(param.getData(), Map.class);
		// clean resolve data
		param.setData(null);

		if (data != null && !data.isEmpty()) {
			int line = Integer.parseInt(data.containsKey(CompletionResolverUtils.KEY_LINE) ? (String)data.get(CompletionResolverUtils.KEY_LINE) : "0");
			int start = Integer.parseInt(data.containsKey(CompletionResolverUtils.KEY_REPLACE_RANGE_START) ? (String)data.get(CompletionResolverUtils.KEY_REPLACE_RANGE_START) : "0");
			int end = Integer.parseInt(data.containsKey(CompletionResolverUtils.KEY_REPLACE_RANGE_END) ? (String)data.get(CompletionResolverUtils.KEY_REPLACE_RANGE_END) : "0");
			String replacement = (String)data.get(CompletionResolverUtils.KEY_REPLACEMENT);
			
			Position pStart = new Position(line, start);
			Position pEnd = new Position(line, end);
			
			Range range = new Range(pStart, pEnd);
			
			if (data.containsKey(CompletionResolverUtils.KEY_LINE) && 
				data.containsKey(CompletionResolverUtils.KEY_REPLACE_RANGE_START) &&
				data.containsKey(CompletionResolverUtils.KEY_REPLACE_RANGE_END) && 
				data.containsKey(CompletionResolverUtils.KEY_REPLACEMENT)) {
				// replace instead of insert
				param.setTextEdit(new TextEdit(range, replacement != null ? replacement : param.getLabel()));				
			}			
		}

		// stay with insert
		return param;
	}
}
