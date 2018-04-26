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
package org.apache.camel.tools.lsp.internal.parser;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;

import org.junit.Test;

import com.github.cameltooling.lsp.internal.instancemodel.CamelURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.OptionParamURIInstance;
import com.github.cameltooling.lsp.internal.instancemodel.PathParamURIInstance;

public class CamelURIInstanceTest {
	
	@Test
	public void testEmptyUri() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("");
		assertThat(camelURIInstance.getComponent()).isNull();
		assertThat(camelURIInstance.getOptionParams()).isEmpty();
		assertThat(camelURIInstance.getOptionParams()).isEmpty();
	}
	
	@Test
	public void testComponentOnlyInUri() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer");
		assertThat(camelURIInstance.getComponent().getComponentName()).isEqualTo("timer");
		assertThat(camelURIInstance.getComponent().getEndPosition()).isEqualTo(5);
	}
	
	@Test
	public void testComponentWithSomethingElseInUri() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName");
		assertThat(camelURIInstance.getComponent().getComponentName()).isEqualTo("timer");
		assertThat(camelURIInstance.getComponent().getEndPosition()).isEqualTo(5);
	}
	
	@Test
	public void testPathParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName");
		PathParamURIInstance pathParam = camelURIInstance.getPathParams().iterator().next();
		assertThat(pathParam.getValue()).isEqualTo("timerName");
		assertThat(pathParam.getStartPosition()).isEqualTo(6);
		assertThat(pathParam.getEndPosition()).isEqualTo(15);
	}
	
	@Test
	public void testMultiplePathParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("amqp:destinationType:destinationName");
		assertThat(camelURIInstance.getPathParams()).containsOnly(
				new PathParamURIInstance("destinationType", 5, 20),
				new PathParamURIInstance("destinationName", 21, 36));
	}
	
	@Test
	public void testMultiplePathParamWithSomethingElseInUri() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("amqp:destinationType:destinationName?anOption");
		assertThat(camelURIInstance.getPathParams()).containsOnly(
				new PathParamURIInstance("destinationType", 5, 20),
				new PathParamURIInstance("destinationName", 21, 36));
	}
	
	@Test
	public void testMultiplePathParamWithSlashDelimiter() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("atmos:name/operation");
		assertThat(camelURIInstance.getPathParams()).containsOnly(
				new PathParamURIInstance("name", 6, 10),
				new PathParamURIInstance("operation", 11, 20));
	}
	
	@Test
	public void testOptionParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay=1000");
		OptionParamURIInstance optionParam = camelURIInstance.getOptionParams().iterator().next();
		checkDelayTimerParam(optionParam);
	}

	private void checkDelayTimerParam(OptionParamURIInstance optionParam) {
		assertThat(optionParam.getStartPosition()).isEqualTo(16);
		assertThat(optionParam.getEndPosition()).isEqualTo(26);
		assertThat(optionParam.getKey().getKeyName()).isEqualTo("delay");
		assertThat(optionParam.getKey().getStartPosition()).isEqualTo(16);
		assertThat(optionParam.getKey().getEndPosition()).isEqualTo(21);
		assertThat(optionParam.getValue().getValueName()).isEqualTo("1000");
		assertThat(optionParam.getValue().getStartPosition()).isEqualTo(22);
		assertThat(optionParam.getValue().getEndPosition()).isEqualTo(26);
	}
	
	@Test
	public void testSeveralOptionParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay=1000&amp;period=2000");
		Iterator<OptionParamURIInstance> iterator = camelURIInstance.getOptionParams().iterator();
		OptionParamURIInstance firstOptionParam = iterator.next();
		OptionParamURIInstance secondOptionParam = iterator.next();
		if("delay".equals(firstOptionParam.getKey().getKeyName())) {
			checkDelayTimerParam(firstOptionParam);
			checkTimerPeriodParam(secondOptionParam);
		} else {
			checkDelayTimerParam(secondOptionParam);
			checkTimerPeriodParam(firstOptionParam);
		}
	}

	private void checkTimerPeriodParam(OptionParamURIInstance secondOptionParam) {
		assertThat(secondOptionParam.getStartPosition()).isEqualTo(31);
		assertThat(secondOptionParam.getEndPosition()).isEqualTo(42);
		assertThat(secondOptionParam.getKey().getKeyName()).isEqualTo("period");
		assertThat(secondOptionParam.getKey().getStartPosition()).isEqualTo(31);
		assertThat(secondOptionParam.getKey().getEndPosition()).isEqualTo(37);
		assertThat(secondOptionParam.getValue().getValueName()).isEqualTo("2000");
		assertThat(secondOptionParam.getValue().getStartPosition()).isEqualTo(38);
		assertThat(secondOptionParam.getValue().getEndPosition()).isEqualTo(42);
	}
	
	@Test
	public void testEmptyOptionParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?");
		OptionParamURIInstance optionParam = camelURIInstance.getOptionParams().iterator().next();
		assertThat(optionParam.getStartPosition()).isEqualTo(16);
		assertThat(optionParam.getEndPosition()).isEqualTo(16);
		assertThat(optionParam.getKey().getKeyName()).isEqualTo("");
		assertThat(optionParam.getKey().getStartPosition()).isEqualTo(16);
		assertThat(optionParam.getKey().getEndPosition()).isEqualTo(16);
		assertThat(optionParam.getValue()).isNull();
	}
	
	@Test
	public void testEmptyOptionValueParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay");
		OptionParamURIInstance optionParam = camelURIInstance.getOptionParams().iterator().next();
		assertThat(optionParam.getStartPosition()).isEqualTo(16);
		assertThat(optionParam.getEndPosition()).isEqualTo(21);
		assertThat(optionParam.getKey().getKeyName()).isEqualTo("delay");
		assertThat(optionParam.getKey().getStartPosition()).isEqualTo(16);
		assertThat(optionParam.getKey().getEndPosition()).isEqualTo(21);
		assertThat(optionParam.getValue()).isNull();
	}
	
	@Test
	public void testEmptyOptionValueParamWithAnd() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay&amp;");
		Iterator<OptionParamURIInstance> iterator = camelURIInstance.getOptionParams().iterator();
		OptionParamURIInstance firstOptionParam = iterator.next();
		OptionParamURIInstance secondOptionParam = iterator.next();
		if("delay".equals(firstOptionParam.getKey().getKeyName())) {
			checkDelayParameter(firstOptionParam);
		} else {
			checkDelayParameter(secondOptionParam);
		}
	}

	private void checkDelayParameter(OptionParamURIInstance firstOptionParam) {
		assertThat(firstOptionParam.getStartPosition()).isEqualTo(16);
		assertThat(firstOptionParam.getEndPosition()).isEqualTo(21);
		assertThat(firstOptionParam.getKey().getKeyName()).isEqualTo("delay");
		assertThat(firstOptionParam.getKey().getStartPosition()).isEqualTo(16);
		assertThat(firstOptionParam.getKey().getEndPosition()).isEqualTo(21);
		assertThat(firstOptionParam.getValue()).isNull();
	}
	
	@Test
	public void testEmptyOptionValueParamWithEqual() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay=");
		OptionParamURIInstance optionParam = camelURIInstance.getOptionParams().iterator().next();
		assertThat(optionParam.getStartPosition()).isEqualTo(16);
		assertThat(optionParam.getEndPosition()).isEqualTo(22);
		assertThat(optionParam.getKey().getKeyName()).isEqualTo("delay");
		assertThat(optionParam.getKey().getStartPosition()).isEqualTo(16);
		assertThat(optionParam.getKey().getEndPosition()).isEqualTo(21);
		assertThat(optionParam.getValue().getValueName()).isEqualTo(null);
		assertThat(optionParam.getValue().getStartPosition()).isEqualTo(22);
		assertThat(optionParam.getValue().getEndPosition()).isEqualTo(22);
	}

}
