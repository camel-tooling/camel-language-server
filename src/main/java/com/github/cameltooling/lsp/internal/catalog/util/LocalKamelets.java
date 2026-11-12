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
package com.github.cameltooling.lsp.internal.catalog.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.camel.v1.Kamelet;
import org.eclipse.lsp4j.TextDocumentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * The Kamelets of the application: the <code>*.kamelet.yaml</code> files of the local Kamelet directories, which
 * Camel finds by name like the <code>--local-kamelet-dir</code> of Camel JBang. A relative directory is resolved
 * against the directory of the document being edited. The unsaved content of an opened Kamelet wins over its file.
 */
public class LocalKamelets {

	private static final String KAMELET_FILE_SUFFIX = ".kamelet.yaml";
	private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile("^[a-zA-Z]:[\\\\/].*");
	private static final Logger LOGGER = LoggerFactory.getLogger(LocalKamelets.class);
	private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory()).configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private final Supplier<Collection<TextDocumentItem>> openedDocuments;
	private final Map<Path, CachedKamelet> cache = new ConcurrentHashMap<>();
	private volatile List<String> directories = Collections.emptyList();

	public LocalKamelets(Supplier<Collection<TextDocumentItem>> openedDocuments) {
		this.openedDocuments = openedDocuments;
	}

	public void setDirectories(List<String> directories) {
		this.directories = directories != null ? List.copyOf(directories) : Collections.emptyList();
	}

	/**
	 * @param documentUri the document being edited, against which the relative directories are resolved; null to use the absolute directories only
	 * @return the local Kamelets by name, the name being the one Camel resolves: the file name without <code>.kamelet.yaml</code>.
	 * With the same name in several directories, the first directory wins.
	 */
	public Map<String, Kamelet> getKamelets(String documentUri) {
		List<Path> resolvedDirectories = resolveDirectories(documentUri);
		if (resolvedDirectories.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<Path, String> openedKamelets = getOpenedKamelets();
		Map<String, Kamelet> kamelets = new LinkedHashMap<>();
		for (Path directory : resolvedDirectories) {
			readDirectory(directory, openedKamelets).forEach(kamelets::putIfAbsent);
		}
		return kamelets;
	}

	private List<Path> resolveDirectories(String documentUri) {
		Path documentDirectory = toPath(documentUri).map(Path::getParent).orElse(null);
		List<Path> resolved = new ArrayList<>();
		for (String directory : directories) {
			toPath(directory)
				.map(path -> path.isAbsolute() ? path : documentDirectory != null ? documentDirectory.resolve(path) : null)
				.map(Path::normalize)
				.filter(path -> !resolved.contains(path))
				.ifPresent(resolved::add);
		}
		return resolved;
	}

	private Map<String, Kamelet> readDirectory(Path directory, Map<Path, String> openedKamelets) {
		Set<Path> files = listKameletFiles(directory);
		Map<String, Kamelet> kamelets = new HashMap<>();
		for (Path file : files) {
			String openedContent = openedKamelets.get(file.toAbsolutePath().normalize());
			Kamelet kamelet = openedContent != null ? parse(openedContent, file) : load(file);
			if (kamelet != null) {
				kamelets.put(getName(file), kamelet);
			}
		}
		// forget the files that were deleted
		cache.keySet().removeIf(cached -> directory.equals(cached.getParent()) && !files.contains(cached));
		return kamelets;
	}

	private static Set<Path> listKameletFiles(Path directory) {
		try (Stream<Path> files = Files.list(directory)) {
			return files.filter(LocalKamelets::isKameletFile).collect(Collectors.toSet());
		} catch (NoSuchFileException | NotDirectoryException e) {
			// no local Kamelets there
		} catch (IOException e) {
			LOGGER.warn("Cannot read the local Kamelets of {}", directory, e);
		}
		return Collections.emptySet();
	}

	private Kamelet load(Path file) {
		try {
			BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
			long lastModified = attributes.lastModifiedTime().toMillis();
			long size = attributes.size();
			CachedKamelet cached = cache.get(file);
			if (cached != null && cached.lastModified() == lastModified && cached.size() == size) {
				return cached.kamelet();
			}
			Kamelet kamelet = parse(Files.readString(file), file);
			cache.put(file, new CachedKamelet(lastModified, size, kamelet));
			return kamelet;
		} catch (IOException e) {
			LOGGER.debug("Cannot read the local Kamelet {}", file, e);
			return null;
		}
	}

	/**
	 * @return the Kamelet, or null if the content is not a Kamelet with a definition yet, such as a Kamelet being written
	 */
	private static Kamelet parse(String content, Path file) {
		try {
			Kamelet kamelet = MAPPER.readValue(content, Kamelet.class);
			if (kamelet != null && kamelet.getSpec() != null && kamelet.getSpec().getDefinition() != null) {
				return kamelet;
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.debug("Cannot parse the local Kamelet {}", file, e);
		}
		return null;
	}

	private Map<Path, String> getOpenedKamelets() {
		Map<Path, String> opened = new HashMap<>();
		for (TextDocumentItem document : openedDocuments.get()) {
			if (document.getUri() != null && document.getUri().endsWith(KAMELET_FILE_SUFFIX)) {
				toPath(document.getUri()).filter(Path::isAbsolute).ifPresent(path -> opened.put(path.normalize(), document.getText()));
			}
		}
		return opened;
	}

	private static boolean isKameletFile(Path file) {
		return file.getFileName().toString().endsWith(KAMELET_FILE_SUFFIX) && Files.isRegularFile(file);
	}

	private static String getName(Path file) {
		String fileName = file.getFileName().toString();
		return fileName.substring(0, fileName.length() - KAMELET_FILE_SUFFIX.length());
	}

	/**
	 * @return the path of a file URI or of a file system path
	 */
	private static Optional<Path> toPath(String location) {
		if (location == null || location.isBlank()) {
			return Optional.empty();
		}
		try {
			if (location.startsWith("file:")) {
				return Optional.of(Paths.get(URI.create(location)));
			}
			if (location.contains(":") && !WINDOWS_ABSOLUTE_PATH.matcher(location).matches()) {
				// another URI scheme, such as untitled: or vscode-remote:, or a document URI without file system path
				return Optional.empty();
			}
			return Optional.of(Paths.get(location));
		} catch (IllegalArgumentException | FileSystemNotFoundException e) {
			return Optional.empty();
		}
	}

	/** A parsed Kamelet file, kept until the file changes; the Kamelet is null if the file is not a Kamelet. */
	private record CachedKamelet(long lastModified, long size, Kamelet kamelet) {
	}
}
