package com.github.cameltooling.lsp.internal.completion.modeline;

import com.github.cameltooling.lsp.internal.parser.CamelKModelineInsertionParser;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CamelKModelineInsertionProcessor {

    private final TextDocumentItem textDocumentItem;

    public CamelKModelineInsertionProcessor(TextDocumentItem textDocumentItem) {
        this.textDocumentItem = textDocumentItem;
    }

    public CompletableFuture<List<CompletionItem>> getCompletions() {
        return CompletableFuture.completedFuture(
                Arrays.asList(
                        getCompletionCorrespondingToDocument()
                ));
    }

    private CompletionItem getCompletionCorrespondingToDocument() {
        return FileType.getFileTypeCorrespondingToUri(textDocumentItem.getUri()).orElseThrow().completion;
    }

    private enum FileType {
        XML(List.of(".camelk.xml"), "<!-- camel-k: -->", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
        Java(List.of(".java"),"// camel-k:", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
        YAML(List.of(".camelk.yaml",".camelk.yml"),"# camel-k", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html");

        public final List<String> correspondingExtensions;
        public final CompletionItem completion;

        private FileType(List<String> correspondingExtensions, String completionLabel, String completionDocumentation) {
            this.correspondingExtensions = correspondingExtensions;
            this.completion = getCompletionItem(completionLabel, completionDocumentation);
        }

        private static CompletionItem getCompletionItem(String label, String documentation) {
            CompletionItem completion = new CompletionItem(label);
            completion.setDocumentation(documentation);

            return completion;
        }

        private static Optional<FileType> getFileTypeCorrespondingToUri(String uri) {
            return Arrays.asList(FileType.values()).stream()
                    .filter(type ->
                            type.correspondingExtensions.stream().anyMatch(uri::endsWith)
                    )
                    .findFirst();
        }
    }
}
