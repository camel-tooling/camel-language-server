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

import java.util.List;

import org.apache.camel.tooling.model.EipModel;
import org.apache.camel.tooling.model.EipModel.EipOptionModel;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;

import com.github.cameltooling.lsp.internal.catalog.runtimeprovider.RuntimeConfigurationProperty;

/**
 * Builds the documentation of Camel catalog models (EIPs, expression languages, data formats) and of their options,
 * and of the configuration properties of the runtimes.
 */
public final class ModelDocumentationHelper {

	private static final String MARKDOWN_LINE_BREAK = "\\\n";

	private ModelDocumentationHelper() {
		// util class
	}

	public static MarkupContent getModelDocumentation(EipModel model, boolean markdown) {
		StringBuilder doc = new StringBuilder();
		String title = model.getTitle() != null ? model.getTitle() : model.getName();
		doc.append(markdown ? bold(title) : title);
		if (model.getLabel() != null && !model.getLabel().isEmpty()) {
			doc.append(" (").append(model.getLabel()).append(")");
		}
		appendDescription(doc, model.getDescription());
		if (model.isDeprecated()) {
			doc.append("\n\n").append(markdown ? bold("Deprecated") : "Deprecated");
		}
		return new MarkupContent(markdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT, doc.toString());
	}

	public static MarkupContent getOptionDocumentation(String name, EipOptionModel option, boolean markdown) {
		StringBuilder doc = new StringBuilder();
		doc.append(markdown ? bold(name) : name);
		appendDescription(doc, option.getDescription());
		StringBuilder details = new StringBuilder();
		if (option.isRequired()) {
			appendDetail(details, "Required", "true", markdown);
		}
		String type = option.getJavaType() != null ? option.getJavaType() : option.getType();
		appendDetail(details, "Type", type, markdown);
		appendDetail(details, "Default value", option.getDefaultValue() != null ? option.getDefaultValue().toString() : null, markdown);
		List<String> enums = option.getEnums();
		if (enums != null && !enums.isEmpty()) {
			appendDetail(details, "Possible values", String.join(", ", enums), markdown);
		}
		if (option.isDeprecated()) {
			appendDetail(details, "Deprecated", "true", markdown);
		}
		if (details.length() > 0) {
			doc.append("\n\n").append(details);
		}
		return new MarkupContent(markdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT, doc.toString().trim());
	}

	public static MarkupContent getConfigurationPropertyDocumentation(RuntimeConfigurationProperty property, boolean markdown) {
		StringBuilder doc = new StringBuilder();
		doc.append(markdown ? bold(property.getName()) : property.getName());
		appendDescription(doc, property.getDescription());
		StringBuilder details = new StringBuilder();
		appendDetail(details, "Type", property.getType(), markdown);
		appendDetail(details, "Default value", property.getDefaultValue(), markdown);
		if (!property.getEnumValues().isEmpty()) {
			appendDetail(details, "Possible values", String.join(", ", property.getEnumValues()), markdown);
		}
		if (property.isDeprecated()) {
			appendDetail(details, "Deprecated", property.getDeprecationNote().isEmpty() ? "true" : property.getDeprecationNote(), markdown);
		}
		appendDetail(details, "Runtime", property.getRuntime(), markdown);
		doc.append("\n\n").append(details);
		return new MarkupContent(markdown ? MarkupKind.MARKDOWN : MarkupKind.PLAINTEXT, doc.toString().trim());
	}

	private static void appendDescription(StringBuilder doc, String description) {
		if (description != null && !description.isEmpty()) {
			doc.append("\n\n").append(description);
		}
	}

	private static void appendDetail(StringBuilder details, String label, String value, boolean markdown) {
		if (value != null && !value.isEmpty()) {
			// a Markdown line break ending the documentation would show as a backslash
			if (details.length() > 0) {
				details.append(markdown ? MARKDOWN_LINE_BREAK : "\n");
			}
			details.append(markdown ? bold(label + ":") : label + ":").append(' ').append(value);
		}
	}

	private static String bold(String text) {
		return "**" + text + "**";
	}
}
