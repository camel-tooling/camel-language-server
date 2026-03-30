package com.github.cameltooling.lsp.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.RuntimeProvider;
import org.apache.camel.catalog.maven.MavenVersionManager;
import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.CamelRuntimeProvider;
import com.github.cameltooling.lsp.internal.settings.JSONUtility;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelCatalogService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelCatalogService.class);

    public CompletableFuture<org.apache.camel.catalog.CamelCatalog> updateCatalog(String camelVersion, String camelCatalogRuntimeProvider, List<Map<?, ?>> extraComponents) {
        return CompletableFuture.supplyAsync(() -> {
            DefaultCamelCatalog catalog = new DefaultCamelCatalog(true);
            updateCatalogVersion(camelVersion, catalog);
            updateCatalogRuntimeProvider(camelCatalogRuntimeProvider, catalog);
            updateCatalogExtraComponents(extraComponents, catalog);
            return catalog;
        });
    }

    private void updateCatalogVersion(String camelVersion, DefaultCamelCatalog catalog) {
        if (camelVersion != null && !camelVersion.isEmpty()) {
            MavenVersionManager versionManager = new MavenVersionManager();
            if (camelVersion.contains("redhat")) {
                versionManager.addMavenRepository("central", "https://repo1.maven.org/maven2/");
                versionManager.addMavenRepository("maven.redhat.ga", "https://maven.repository.redhat.com/ga/");
            }
            catalog.setVersionManager(versionManager);
            if (!catalog.loadVersion(camelVersion)) {
                LOGGER.warn("Cannot load Camel catalog with version {}", camelVersion);
            }
        }
    }

    private void updateCatalogRuntimeProvider(String camelCatalogRuntimeProvider, DefaultCamelCatalog catalog) {
        if(camelCatalogRuntimeProvider != null && !camelCatalogRuntimeProvider.isEmpty()) {
            RuntimeProvider runtimeProvider = CamelRuntimeProvider.getProvider(camelCatalogRuntimeProvider);
            if(runtimeProvider != null) {
                catalog.setRuntimeProvider(runtimeProvider);
            }
        }
    }

    private void updateCatalogExtraComponents(List<Map<?, ?>> extraComponents, DefaultCamelCatalog catalog) {
        if (extraComponents != null) {
            for (Map<?,?> extraComponent : extraComponents) {
                JSONUtility jsonUtility = new JSONUtility();
                Map<?,?> extraComponentTopLevel = jsonUtility.toModel(extraComponent, Map.class);
                Map<?,?> componentAttributes = jsonUtility.toModel(extraComponentTopLevel.get("component"), Map.class);
                String name = (String) componentAttributes.get("scheme");
                String className = (String) componentAttributes.get("javaType");
                catalog.addComponent(name, className, new Gson().toJson(extraComponent));
            }
        }
    }
}