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
package org.apache.camel.tools.lsp.internal;

import java.io.IOException;

import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lhein
 */
public abstract class AbstractLanguageServer {
	
	private static final String OS = System.getProperty("os.name").toLowerCase();
	private static final String ARCH = System.getProperty("os.arch").toLowerCase();
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLanguageServer.class);
	
	private Thread runner;
	private volatile boolean shutdown;
	private long parentProcessId;
	private WorkspaceService workspaceService;
	private TextDocumentService textDocumentService;
	
	/**
	 * starts the language server process
	 * 
	 * @return	the exit code of the process
	 */
	public int startServer() {
		runner = new Thread(new Runnable() {
			@Override
			public void run() {
				LOGGER.info("Starting Camel Language Server...");
				while (!shutdown && parentProcessStillRunning()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// Not expected. Continue.
						LOGGER.error(e.getMessage(), e);
					}
				}
				LOGGER.info("Camel Language Server - Client vanished...");				
			}
		}, "Camel Language Client Watcher");
		runner.start();
		return 0;
	}
	
	/**
	 * Checks whether the parent process is still running.
	 * If not, then we assume it has crashed, and we have to terminate the Camel Language Server.
	 *
	 * @return true if the parent process is still running
	 */
	protected boolean parentProcessStillRunning() {
		// Wait until parent process id is available
		long parentProcessId = getParentProcessId();
		
		if (parentProcessId == 0) {
			LOGGER.info("Waiting for a client connection...");
		} else {
			LOGGER.info("Checking for client process pid: " + parentProcessId);
		}
		
		if (parentProcessId == 0) return true;

		String command;
		if (OS.indexOf("win") != -1) { // && "x86".equals(ARCH)
			command = "cmd /c \"tasklist /FI \"PID eq " + parentProcessId + "\" | findstr " + parentProcessId + "\"";
		} else {
			command = "ps -p " + parentProcessId;
		}
		try {
			Process process = Runtime.getRuntime().exec(command);
			int processResult = process.waitFor();
			return processResult == 0;
		} catch (IOException | InterruptedException e) {
			LOGGER.error(e.getMessage(), e);
			return true;
		}
	}

	/**
	 * stops the server
	 */
	public void stopServer() {
		LOGGER.info("Stopping language server");
	}

	/**
	 * shuts the server down
	 */
	public void shutdownServer() {
		LOGGER.info("Shutting down language server");
		shutdown = true;
	}

	/**
	 * returns the parent process id
	 * 
	 * @return
	 */
	protected synchronized long getParentProcessId() {
		return parentProcessId;
	}

	/**
	 * sets the parent process id
	 * 
	 * @param processId
	 */
	protected synchronized void setParentProcessId(long processId) {
		LOGGER.info("Setting client pid to " + processId);
		parentProcessId = processId;
	}
	
	/**
	 * @return the textDocumentService
	 */
	public TextDocumentService getTextDocumentService() {
		return this.textDocumentService;
	}
	
	/**
	 * @param textDocumentService the textDocumentService to set
	 */
	protected void setTextDocumentService(TextDocumentService textDocumentService) {
		this.textDocumentService = textDocumentService;
	}
	
	/**
	 * @return the workspaceService
	 */
	protected WorkspaceService getWorkspaceService() {
		return this.workspaceService;
	}
	
	/**
	 * @param workspaceService the workspaceService to set
	 */
	protected void setWorkspaceService(WorkspaceService workspaceService) {
		this.workspaceService = workspaceService;
	}
}
