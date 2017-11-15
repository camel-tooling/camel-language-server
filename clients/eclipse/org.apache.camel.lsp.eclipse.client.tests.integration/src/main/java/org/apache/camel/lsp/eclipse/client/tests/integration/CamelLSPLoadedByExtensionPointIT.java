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
package org.apache.camel.lsp.eclipse.client.tests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.genericeditor.ExtensionBasedTextEditor;
import org.junit.Test;

public class CamelLSPLoadedByExtensionPointIT {
	
	@Test
	public void testGenericEditorCanOpenCamelFile() throws Exception {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(CamelLSPLoadedByExtensionPointIT.class.getSimpleName());
		project.create(null);
		project.open(null);
		IFile camelFile = project.getFile("camelFile.xml");
		camelFile.create(new ByteArrayInputStream("<from uri=\"\" xmlns=\"http://camel.apache.org/schema/spring\"></from>\n".getBytes()), IResource.FORCE, null);
		IEditorPart openEditor = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), camelFile, "org.eclipse.ui.genericeditor.GenericEditor");
		assertThat(openEditor).isInstanceOf(ExtensionBasedTextEditor.class);
	}

}
