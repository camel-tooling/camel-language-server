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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunnerStandardIOTest {

	private PrintStream sysOut;
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private Thread thread;

	@BeforeEach
	void beforeClass() {
		sysOut = System.out;
		System.setOut(new PrintStream(outContent));
	}

	@AfterEach
	void afterClass() {
		if(thread != null) {
			thread.interrupt();
			await("The Thread is still alive although interrupt was called.").untilAsserted(() -> assertThat(thread.isAlive()).isFalse());
		}
		System.setOut(sysOut);
	}

	@Test
	void testClientProxyAvailable() throws Exception {
		startRunnerWithoutOption();
		await("Wait for Server to be initialized")
				.untilAsserted(() -> assertThat(Runner.server).isNotNull());
		await("Wait for Server to start with a remote proxy client")
				.untilAsserted(() -> assertThat(Runner.server.getClient()).isNotNull());
		assertThat(outContent.toString()).doesNotContain("help");
	}

	private void startRunnerWithoutOption() {
		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				Runner.main(new String[] {});
			}
		});
		thread.start();
	}
}
