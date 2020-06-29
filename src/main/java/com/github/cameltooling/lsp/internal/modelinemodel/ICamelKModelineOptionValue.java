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
import org.eclipse.lsp4j.Hover;

import com.github.cameltooling.lsp.internal.instancemodel.ILineRangeDefineable;

public interface ICamelKModelineOptionValue extends ILineRangeDefineable {

	public default int getLine() {
		return 0;
	}

	public String getValueAsString();

	public default CompletableFuture<List<CompletionItem>> getCompletions(int position) {
		return CompletableFuture.completedFuture(Collections.emptyList());
	}

	public boolean isInRange(int position);

	public default CompletableFuture<Hover> getHover(int characterPosition) {
		return CompletableFuture.completedFuture(null);
	}

}
