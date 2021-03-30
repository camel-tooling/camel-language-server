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
package com.github.cameltooling.lsp.internal.websocket;

import jakarta.websocket.DeploymentException;

import org.glassfish.tyrus.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketRunner.class);
	
	private static final String DEFAULT_HOSTNAME = "localhost";
	private static final int DEFAULT_PORT = 8025;
	private static final String DEFAULT_CONTEXT_PATH = "/";
	
	private boolean isStarted = false;

	public void runWebSocketServer(String hostname, int port, String contextPath) {
		hostname = hostname != null ? hostname : DEFAULT_HOSTNAME;
		port = port != -1 ? port : DEFAULT_PORT;
		contextPath = contextPath != null ? contextPath : DEFAULT_CONTEXT_PATH;
		Server server = new Server(hostname, port, contextPath, null, CamelLSPWebSocketServerConfigProvider.class);
		Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "camel-lsp-websocket-server-shutdown-hook"));

		try {
			server.start();
			isStarted = true;
			Thread.currentThread().join();
		} catch (InterruptedException e) {
			LOGGER.error("Camel LSP Websocket server has been interrupted.", e);
			Thread.currentThread().interrupt();
		} catch (DeploymentException e) {
			LOGGER.error("Cannot start Camel LSP Websocket server.", e);
		} finally {
			server.stop();
		}
	}

	public boolean isStarted() {
		return isStarted;
	}

}
