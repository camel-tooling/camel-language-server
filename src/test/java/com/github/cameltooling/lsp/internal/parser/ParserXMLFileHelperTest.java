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
package com.github.cameltooling.lsp.internal.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

public class ParserXMLFileHelperTest {

	@Test
	public void testGetCamelComponentUri() throws Exception {
		final TestAppender appender = new TestAppender();
		final Logger logger = Logger.getRootLogger();
        logger.addAppender(appender);
		new ParserXMLFileHelper().getCamelComponentUri("uri=!!", 2);
		assertThat(appender.getLog().get(0).getMessage()).isEqualTo("Encountered an unsupported URI closure char !");
	}
	
	class TestAppender extends AppenderSkeleton {
	    private final List<LoggingEvent> log = new ArrayList<LoggingEvent>();

	    @Override
	    public boolean requiresLayout() {
	        return false;
	    }

	    @Override
	    protected void append(final LoggingEvent loggingEvent) {
	        log.add(loggingEvent);
	    }

	    @Override
	    public void close() {
	    }

	    public List<LoggingEvent> getLog() {
	        return new ArrayList<LoggingEvent>(log);
	    }
	}

}
