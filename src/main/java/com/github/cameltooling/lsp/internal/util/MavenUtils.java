/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.github.cameltooling.lsp.internal.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author lheinema
 *
 */
public class MavenUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MavenUtils.class);
	
	private static final String CAMEL_GID = "org.apache.camel";
	private static final String CAMEL_AID_PREFIX = "camel-";
	
	private MavenUtils() {
		// util class
	}
	
	public static Model getModelFromPOM(File pom) throws XmlPullParserException, IOException {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		return reader.read(new FileReader(pom));
	}
	
	public static String retrieveProjectCamelVersion(String docUri, String fallBackCamelVersion) {
		File pom = findPomInReferencedURI(docUri);
		if (pom != null) {
			try {
				Model model = getModelFromPOM(pom);
				if (model.getDependencies() != null) {
					Dependency dep = getAnyCamelDependencyFromDependencies(model);
					if (dep == null && model.getDependencyManagement() != null) {
						dep = getAnyCamelDependencyFromDependencyManagement(model);
					}
					if (dep != null) {
						return dep.getVersion();
					}
				}
			} catch (XmlPullParserException | IOException ex) {
				LOGGER.error("Error retrieving the Camel version from the project.", ex);
			}
		}
		return fallBackCamelVersion;
	}

	public static boolean isDependencyConfiguredInPom(Model model, Dependency dependency) {
		return getDependencyFromModel(model, dependency) != null;
	}
	
	public static Dependency getDependencyFromModel(Model model, Dependency dependency) {
		Dependency dep = getDependencyFromDependenciesSection(model, dependency);
		if (dep == null) {
			dep = getDependencyFromDependencyManagementSection(model, dependency);
		}
		return dep;
	}
	
	public static Dependency getDependencyFromDependenciesSection(Model model, Dependency dependency) {
		if (model != null && model.getDependencies() != null) {
			for (Dependency d : model.getDependencies()) {
				if (d.getGroupId().equals(dependency.getGroupId()) &&
					d.getArtifactId().equals(dependency.getArtifactId())) {
					return d;
				}
			}
		}
		return null;
	}
	
	public static Dependency getDependencyFromDependencyManagementSection(Model model, Dependency dependency) {
		if (model != null && model.getDependencyManagement() != null && model.getDependencyManagement().getDependencies() != null) {
			for (Dependency d : model.getDependencyManagement().getDependencies()) {
				if (d.getGroupId().equals(dependency.getGroupId()) &&
					d.getArtifactId().equals(dependency.getArtifactId())) {
					return d;
				}
			}
		}
		return null;
	}
	
	public static Dependency getAnyCamelDependencyFromDependencies(Model model) {
		for (Dependency dep : model.getDependencies()) {
			if (dep.getGroupId().equals(CAMEL_GID) && dep.getArtifactId().startsWith(CAMEL_AID_PREFIX) && dep.getVersion() != null) {
				return dep;
			}
		}
		return null;
	}

	public static Dependency getAnyCamelDependencyFromDependencyManagement(Model model) {
		for (Dependency dep : model.getDependencyManagement().getDependencies()) {
			if (dep.getGroupId().equals(CAMEL_GID) && dep.getArtifactId().startsWith(CAMEL_AID_PREFIX) && dep.getVersion() != null) {
				return dep;
			}
		}
		return null;
	}

	public static File findPomInReferencedURI(String docUri) {
		File pom = null;
		URI uri = URI.create(docUri);
		File f = new File(uri.getPath());
		while (f.getParentFile() != null && pom == null) {
			f = f.getParentFile();
			pom = getPomFromPath(f);
		}
		return pom;
	}

	public static File getPomFromPath(File path) {
		File[] files = path.listFiles( (File pathname) ->
			"pom.xml".equalsIgnoreCase(pathname.getName())
		);
		if (files.length>0) {
			return files[0];
		}
		return null;
 	}
	
	public static boolean isComponentDependencyConfigured(File pom, CamelCatalog camelCatalog, String componentName) {
		if (pom != null) {
			try {
				Model model = getModelFromPOM(pom);
				Dependency componentDependency = getComponentDependencyFromCatalog(camelCatalog, componentName);
				return isDependencyConfiguredInPom(model, componentDependency);
			} catch (XmlPullParserException | IOException ex) {
				LOGGER.error("Error retrieving the Maven Model from the project.", ex);
			}
		}
		return false;
	}
	
	public static Dependency getComponentDependencyFromCatalog(CamelCatalog catalog, String componentName) {
		Dependency dep = new Dependency();
		String componentJSON = catalog.getJSonSchemaResolver().getComponentJSonSchema(componentName);
		List<Map<String, String>> rows = JSonSchemaHelper.parseJsonSchema("component", componentJSON, false);
		for (Map<String, String> m : rows) {
			Iterator<String> keyIt = m.keySet().iterator();
			while (keyIt.hasNext()) {
				String key = keyIt.next();
				if ("groupId".equalsIgnoreCase(key)) {
					dep.setGroupId(m.get(key));
				} else if ("artifactId".equalsIgnoreCase(key)) {
					dep.setArtifactId(m.get(key));
				}
			}
		}
		return dep;
	}
}
