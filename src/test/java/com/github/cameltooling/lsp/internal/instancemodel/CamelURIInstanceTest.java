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
package com.github.cameltooling.lsp.internal.instancemodel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;

import org.junit.Test;
import org.w3c.dom.Node;

public class CamelURIInstanceTest {
	
	@Test
	public void testEmptyUri() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("", (Node) null);
		assertThat(camelURIInstance.getComponentAndPathUriElementInstance()).isNull();
		assertThat(camelURIInstance.getOptionParams()).isEmpty();
		assertThat(camelURIInstance.getOptionParams()).isEmpty();
	}
	
	@Test
	public void testComponentOnlyInUri() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer", (Node) null);
		assertThat(camelURIInstance.getComponentAndPathUriElementInstance().getComponentName()).isEqualTo("timer");
		assertThat(camelURIInstance.getComponentAndPathUriElementInstance().getEndPositionInUri()).isEqualTo(5);
	}
	
	@Test
	public void testComponentWithSomethingElseInUri() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName", (Node) null);
		assertThat(camelURIInstance.getComponentAndPathUriElementInstance().getComponentName()).isEqualTo("timer");
		assertThat(camelURIInstance.getComponentAndPathUriElementInstance().getComponent().getEndPositionInUri()).isEqualTo(5);
	}
	
	@Test
	public void testPathParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName", (Node) null);
		PathParamURIInstance pathParam = camelURIInstance.getComponentAndPathUriElementInstance().getPathParams().iterator().next();
		assertThat(pathParam.getValue()).isEqualTo("timerName");
		assertThat(pathParam.getStartPositionInUri()).isEqualTo(6);
		assertThat(pathParam.getEndPositionInUri()).isEqualTo(15);
	}
	
	@Test
	public void testMultiplePathParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("amqp:destinationType:destinationName", (Node) null);
		assertThat(camelURIInstance.getComponentAndPathUriElementInstance().getPathParams()).containsOnly(
				new PathParamURIInstance(camelURIInstance.getComponentAndPathUriElementInstance(), "destinationType", 5, 20),
				new PathParamURIInstance(camelURIInstance.getComponentAndPathUriElementInstance(), "destinationName", 21, 36));
	}
	
	@Test
	public void testMultiplePathParamWithSomethingElseInUri() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("amqp:destinationType:destinationName?anOption", (Node) null);
		assertThat(camelURIInstance.getComponentAndPathUriElementInstance().getPathParams()).containsOnly(
				new PathParamURIInstance(camelURIInstance.getComponentAndPathUriElementInstance(), "destinationType", 5, 20),
				new PathParamURIInstance(camelURIInstance.getComponentAndPathUriElementInstance(), "destinationName", 21, 36));
	}
	
	@Test
	public void testMultiplePathParamWithSlashDelimiter() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("atmos:name/operation", (Node) null);
		assertThat(camelURIInstance.getComponentAndPathUriElementInstance().getPathParams()).containsOnly(
				new PathParamURIInstance(camelURIInstance.getComponentAndPathUriElementInstance(), "name", 6, 10),
				new PathParamURIInstance(camelURIInstance.getComponentAndPathUriElementInstance(), "operation", 11, 20));
	}
	
	@Test
	public void testOptionParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay=1000", (Node) null);
		OptionParamURIInstance optionParam = camelURIInstance.getOptionParams().iterator().next();
		checkDelayTimerParam(optionParam);
	}

	private void checkDelayTimerParam(OptionParamURIInstance optionParam) {
		assertThat(optionParam.getStartPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getEndPositionInUri()).isEqualTo(26);
		assertThat(optionParam.getKey().getKeyName()).isEqualTo("delay");
		assertThat(optionParam.getKey().getStartPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getKey().getEndPositionInUri()).isEqualTo(21);
		assertThat(optionParam.getValue().getValueName()).isEqualTo("1000");
		assertThat(optionParam.getValue().getStartPositionInUri()).isEqualTo(22);
		assertThat(optionParam.getValue().getEndPositionInUri()).isEqualTo(26);
	}
	
	@Test
	public void testSeveralOptionParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay=1000&amp;period=2000", (Node) null);
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
	
	@Test
	public void testSeveralOptionParamForJava() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay=1000&period=2000", (String) null);
		Iterator<OptionParamURIInstance> iterator = camelURIInstance.getOptionParams().iterator();
		OptionParamURIInstance firstOptionParam = iterator.next();
		OptionParamURIInstance secondOptionParam = iterator.next();
		if("delay".equals(firstOptionParam.getKey().getKeyName())) {
			checkDelayTimerParam(firstOptionParam);
			checkTimerPeriodParamForJava(secondOptionParam);
		} else {
			checkDelayTimerParam(secondOptionParam);
			checkTimerPeriodParamForJava(firstOptionParam);
		}
	}

	private void checkTimerPeriodParam(OptionParamURIInstance secondOptionParam) {
		assertThat(secondOptionParam.getStartPositionInUri()).isEqualTo(31);
		assertThat(secondOptionParam.getEndPositionInUri()).isEqualTo(42);
		assertThat(secondOptionParam.getKey().getKeyName()).isEqualTo("period");
		assertThat(secondOptionParam.getKey().getStartPositionInUri()).isEqualTo(31);
		assertThat(secondOptionParam.getKey().getEndPositionInUri()).isEqualTo(37);
		assertThat(secondOptionParam.getValue().getValueName()).isEqualTo("2000");
		assertThat(secondOptionParam.getValue().getStartPositionInUri()).isEqualTo(38);
		assertThat(secondOptionParam.getValue().getEndPositionInUri()).isEqualTo(42);
	}
	
	private void checkTimerPeriodParamForJava(OptionParamURIInstance secondOptionParam) {
		assertThat(secondOptionParam.getStartPositionInUri()).isEqualTo(27);
		assertThat(secondOptionParam.getEndPositionInUri()).isEqualTo(38);
		assertThat(secondOptionParam.getKey().getKeyName()).isEqualTo("period");
		assertThat(secondOptionParam.getKey().getStartPositionInUri()).isEqualTo(27);
		assertThat(secondOptionParam.getKey().getEndPositionInUri()).isEqualTo(33);
		assertThat(secondOptionParam.getValue().getValueName()).isEqualTo("2000");
		assertThat(secondOptionParam.getValue().getStartPositionInUri()).isEqualTo(34);
		assertThat(secondOptionParam.getValue().getEndPositionInUri()).isEqualTo(38);
	}
	
	@Test
	public void testEmptyOptionParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?", (Node) null);
		OptionParamURIInstance optionParam = camelURIInstance.getOptionParams().iterator().next();
		assertThat(optionParam.getStartPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getEndPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getKey().getKeyName()).isEqualTo("");
		assertThat(optionParam.getKey().getStartPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getKey().getEndPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getValue()).isNull();
	}
	
	@Test
	public void testEmptyOptionValueParam() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay", (Node) null);
		OptionParamURIInstance optionParam = camelURIInstance.getOptionParams().iterator().next();
		assertThat(optionParam.getStartPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getEndPositionInUri()).isEqualTo(21);
		assertThat(optionParam.getKey().getKeyName()).isEqualTo("delay");
		assertThat(optionParam.getKey().getStartPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getKey().getEndPositionInUri()).isEqualTo(21);
		assertThat(optionParam.getValue()).isNull();
	}
	
	@Test
	public void testEmptyOptionValueParamWithAnd() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay&amp;", (Node) null);
		Iterator<OptionParamURIInstance> iterator = camelURIInstance.getOptionParams().iterator();
		OptionParamURIInstance firstOptionParam = iterator.next();
		OptionParamURIInstance secondOptionParam = iterator.next();
		if("delay".equals(firstOptionParam.getKey().getKeyName())) {
			checkDelayParameter(firstOptionParam);
		} else {
			checkDelayParameter(secondOptionParam);
		}
	}
	
	@Test
	public void testEmptyOptionValueParamWithAndForJava() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay&", (String) null);
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
		assertThat(firstOptionParam.getStartPositionInUri()).isEqualTo(16);
		assertThat(firstOptionParam.getEndPositionInUri()).isEqualTo(21);
		assertThat(firstOptionParam.getKey().getKeyName()).isEqualTo("delay");
		assertThat(firstOptionParam.getKey().getStartPositionInUri()).isEqualTo(16);
		assertThat(firstOptionParam.getKey().getEndPositionInUri()).isEqualTo(21);
		assertThat(firstOptionParam.getValue()).isNull();
	}
	
	@Test
	public void testEmptyOptionValueParamWithEqual() throws Exception {
		CamelURIInstance camelURIInstance = new CamelURIInstance("timer:timerName?delay=", (Node) null);
		OptionParamURIInstance optionParam = camelURIInstance.getOptionParams().iterator().next();
		assertThat(optionParam.getStartPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getEndPositionInUri()).isEqualTo(22);
		assertThat(optionParam.getKey().getKeyName()).isEqualTo("delay");
		assertThat(optionParam.getKey().getStartPositionInUri()).isEqualTo(16);
		assertThat(optionParam.getKey().getEndPositionInUri()).isEqualTo(21);
		assertThat(optionParam.getValue().getValueName()).isEqualTo(null);
		assertThat(optionParam.getValue().getStartPositionInUri()).isEqualTo(22);
		assertThat(optionParam.getValue().getEndPositionInUri()).isEqualTo(22);
	}

}
