package com.github.cameltooling.lsp.internal.completion.modeline;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CamelKModelineInsertionProcessor {

    private final TextDocumentItem textDocumentItem;

    public CamelKModelineInsertionProcessor(TextDocumentItem textDocumentItem) {
        this.textDocumentItem = textDocumentItem;
    }

    public CompletableFuture<List<CompletionItem>> getInsertion() {
        return CompletableFuture.completedFuture(
                Arrays.asList(
                        getCompletionCorrespondingToDocument()
                ));
    }

    private CompletionItem getCompletionCorrespondingToDocument() {
        //What to throw here?
        return FileType.getFileTypeCorrespondingToUri(textDocumentItem.getUri()).orElseThrow().completion;
    }

    //Unify all this file type information eventually
    private enum FileType {
        //Documentation as
        XML(".camelk.xml", "<!-- camel-k: -->", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
        Java(".java","// camel-k:", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
        YAML(".camelk.yaml","# camel-k", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html");

        public final String correspondingExtension;
        public final CompletionItem completion;

        private FileType(String correspondingExtension, String completionLabel, String completionDocumentation) {
            this.correspondingExtension = correspondingExtension;
            this.completion = getCompletionItem(completionLabel, completionDocumentation);
        }

        private static CompletionItem getCompletionItem(String label, String documentation) {
            CompletionItem completion = new CompletionItem(label);
            completion.setDocumentation(documentation);

            return completion;
        }

        private static Optional<FileType> getFileTypeCorrespondingToUri(String uri) {
            return Arrays.asList(FileType.values()).stream()
                    .filter(type -> uri.endsWith(type.correspondingExtension))
                    .findFirst();
        }
    }
}
