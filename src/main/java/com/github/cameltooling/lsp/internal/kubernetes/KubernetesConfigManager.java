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
package com.github.cameltooling.lsp.internal.kubernetes;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class KubernetesConfigManager {

	private static final KubernetesConfigManager instance = new KubernetesConfigManager();
	
	private static KubernetesClient client;
	
	private KubernetesConfigManager() {}
	
	public static KubernetesConfigManager getInstance() {
		return instance;
	}
	
	public void setClient(KubernetesClient client) {
		KubernetesConfigManager.client = client;
	}
	
	public KubernetesClient getClient() {
		if(client == null) {
			client = new DefaultKubernetesClient();
		}
		return client;
	}
	
}
