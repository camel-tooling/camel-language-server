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

package com.github.cameltooling.lsp.internal.completion.modeline;

import org.eclipse.lsp4j.CompletionItem;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Provides configuration utils associated to File Types while handling camel-k modeline
 *
 * @author joshiraez
 */
public enum CamelKModelineFileType {
    XML(
            List.of(".camelk.xml"),
            "<!-- camel-k:",
            CamelKModelineFileType::xmlCommentPattern,
            "<!-- camel-k: -->",
            "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
    Java(
            List.of(".java"),
            "// camel-k:",
            CamelKModelineFileType::javaCommentPattern,
            "// camel-k: ",
            "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
    YAML(
            List.of(".camelk.yaml",".camelk.yml"),
            "# camel-k:",
            CamelKModelineFileType::yamlCommentPattern,
            "# camel-k: ",
            "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html");

    private final List<String> correspondingExtensions;
    private final String modeline;
    private final Supplier<Pattern> commentRegexSupplier;
    private final CompletionItem completion;

    CamelKModelineFileType(List<String> correspondingExtensions,
                           String modeline,
                           Supplier<Pattern> commentRegexSupplier,
                           String completionLabel,
                           String completionDocumentation) {
        this.correspondingExtensions = correspondingExtensions;
        this.modeline = modeline;
        this.commentRegexSupplier = commentRegexSupplier;
        this.completion = getCompletionItem(completionLabel, completionDocumentation);
    }

    public List<String> getCorrespondingExtensions() {
        return correspondingExtensions;
    }

    public String getModeline() {
        return modeline;
    }

    public Supplier<Pattern> getCommentRegexSupplier() {
        return commentRegexSupplier;
    }

    public CompletionItem getCompletion() {
        return completion;
    }

    public static CompletionItem getCompletionItem(String label, String documentation) {
        CompletionItem completion = new CompletionItem(label);
        completion.setDocumentation(documentation);

        return completion;
    }

    public static Optional<CamelKModelineFileType> getFileTypeCorrespondingToUri(String uri) {
        return List.of(CamelKModelineFileType.values()).stream()
                .filter(type ->
                        type.getCorrespondingExtensions().stream().anyMatch(uri::endsWith)
                )
                .findFirst();
    }

    private static Pattern xmlCommentPattern(){
        //Remove all segments between <!-- and -->. Check if it's empty.
        return Pattern.compile("<!--.*-->");
    }

    private static Pattern yamlCommentPattern(){
        //Remove all segments between # and \n
        return Pattern.compile("#.*\\n");
    }

    private static Pattern javaCommentPattern(){
        //Line Comments: Remove from // to \n
        //Block Comments: Remove from /* to */. Newlines have to be explicitly added
        Pattern lineComment = Pattern.compile("\\/\\/.*\\n");
        Pattern blockComment = Pattern.compile("\\/\\*(?s).*\\*\\/");
        return Pattern.compile(String.format("(%s|%s)",lineComment.pattern(), blockComment.pattern()));
    }
}
