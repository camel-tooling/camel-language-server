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

public class GenericCamelKModelineOptionValue implements ICamelKModelineOptionValue {

	private String value;
	private int startPosition;

	public GenericCamelKModelineOptionValue(String value, int startPosition) {
		this.value = value;
		this.startPosition = startPosition;
	}

	@Override
	public int getStartPositionInLine() {
		return startPosition;
	}

	@Override
	public int getEndPositionInLine() {
		return getStartPositionInLine() + value.length();
	}
	
	public String getValueAsString() {
		return value;
	}
	
	@Override
	public boolean isInRange(int position) {
		return startPosition <= position && position <= getEndPositionInLine();
	}

}
