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
package com.github.cameltooling.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RunnerWebSocketTest {
	
	private CountDownLatch messageLatch = new CountDownLatch(1);
	private Thread thread;
	
	@AfterEach
	void tearDown() {
		if(thread != null) {
			thread.interrupt();
		}
	}
	
	@Test
	void testWebsocketServerStartedWithDefaults() throws Exception {
		String[] arguments = new String[] {"--websocket"};
		String expectedConnectionURI = "ws://localhost:8025/camel-language-server";
		testWebSocketServerConnection(arguments, expectedConnectionURI);
	}
	
	@Test
	void testWebsocketServerStartedWithPortSpecified() throws Exception {
		String[] arguments = new String[] {"--websocket", "--port=8026"};
		String expectedConnectionURI = "ws://localhost:8026/camel-language-server";
		testWebSocketServerConnection(arguments, expectedConnectionURI);
	}
	
	@Test
	void testWebsocketServerStartedWithContextPathSpecified() throws Exception {
		String[] arguments = new String[] {"--websocket", "--contextPath=/test"};
		String expectedConnectionURI = "ws://localhost:8025/test/camel-language-server";
		testWebSocketServerConnection(arguments, expectedConnectionURI);
	}
	
	@Test
	void testWebsocketServerStartedWithHostnameSpecified() throws Exception {
		String localHostname = retrieveLocalHostname();
		String[] arguments = new String[] {"--websocket", "--hostname="+localHostname};
		String expectedConnectionURI = "ws://"+localHostname+":8025/camel-language-server";
		testWebSocketServerConnection(arguments, expectedConnectionURI);
	}

	@Test
	void testWebsocketServerStartedWithHostnameAndPortAndContextPathSpecified() throws Exception {
		String localHostname = retrieveLocalHostname();
		String[] arguments = new String[] {"--websocket", "--hostname="+localHostname, "--port=8027", "--contextPath=/test"};
		String expectedConnectionURI = "ws://"+localHostname+":8027/test/camel-language-server";
		testWebSocketServerConnection(arguments, expectedConnectionURI);
	}
	
	private String retrieveLocalHostname() throws UnknownHostException {
		String localHostname = InetAddress.getLocalHost().getHostName();
		assumeFalse("localhost".equals(localHostname), "The test is inaccurate if the hostname is localhost as it is the default value. The code might still be right but the test becomes useless.");
		return localHostname;
	}

	@Test
	void testWebsocketServerWithInvalidPortSpecified() throws Exception {
		String[] arguments = new String[] {"--websocket", "--port=invalidport"};
		assertThrows(IllegalArgumentException.class, () -> {
			Runner.main(arguments);
		});
	}

	private void testWebSocketServerConnection(String[] arguments, String expectedConnectionURI)
			throws InterruptedException, DeploymentException, IOException, URISyntaxException {
		startRunnerWithWebsocketOption(arguments);
		
		await("WebSocket Server has not been started.").until(() -> {return Runner.webSocketRunner != null && Runner.webSocketRunner.isStarted(); });

		final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
		ClientManager client = ClientManager.createClient();
		client.connectToServer(new Endpoint() {

			@Override
			public void onOpen(Session session, EndpointConfig config) {
				messageLatch.countDown();
			}

		}, cec, new URI(expectedConnectionURI));
		assertThat(messageLatch.getCount()).isZero();
	}

	private Thread startRunnerWithWebsocketOption(String[] arguments) {
		thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				Runner.main(arguments);
			}
		});
		thread.start();
		return thread;
	}

}
