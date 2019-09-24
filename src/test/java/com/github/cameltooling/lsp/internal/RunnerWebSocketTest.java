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

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.junit.jupiter.api.Test;

public class RunnerWebSocketTest {
	
	private CountDownLatch messageLatch = new CountDownLatch(1);
	
	@Test
	public void testWebsocketServerStarted() throws Exception {
		startRunnerWithWebsocketOption();
		
		messageLatch.await(1, TimeUnit.SECONDS);
		
		final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();
        ClientManager client = ClientManager.createClient();
        client.connectToServer(new Endpoint() {

            @Override
            public void onOpen(Session session, EndpointConfig config) {
            	messageLatch.countDown();
            }
            
        }, cec, new URI("ws://localhost:8025/camel-language-server"));
	}

	private void startRunnerWithWebsocketOption() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				Runner.main(new String[] {"--websocket"});
			}
		}).start();
	}

}
