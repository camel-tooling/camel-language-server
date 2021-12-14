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
package com.github.cameltooling.lsp.internal.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class TestLoggerUtil {
	
	public TestLogAppender setupLogAppender(String name) {
		final LoggerContext context = LoggerContext.getContext(false);
		final Configuration config = context.getConfiguration();
		config.getRootLogger().setLevel(Level.ALL);
		final TestLogAppender appender = new TestLogAppender(name);
		appender.start();
		config.addAppender(appender);
		updateLoggers(context, appender, config);
		return appender;
	}

	private void updateLoggers(LoggerContext context, final Appender appender, final Configuration config) {
		final Level level = Level.ALL;
		for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
			loggerConfig.addAppender(appender, level, null);
		}
		config.getRootLogger().addAppender(appender, level, null);
		context.updateLoggers(config);
	}

}
