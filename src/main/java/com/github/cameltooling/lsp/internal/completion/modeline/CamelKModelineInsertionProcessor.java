package com.github.cameltooling.lsp.internal.completion.modeline;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CamelKModelineInsertionProcessor {

    private final TextDocumentItem textDocumentItem;

    public CamelKModelineInsertionProcessor(TextDocumentItem textDocumentItem) {
        this.textDocumentItem = textDocumentItem;
    }

    public CompletableFuture<List<CompletionItem>> getCompletions() {
        return CompletableFuture.completedFuture(
                List.of(
                        getCompletionCorrespondingToDocument()
                ));
    }

    private CompletionItem getCompletionCorrespondingToDocument() {
        return CamelKModelineFileType.getFileTypeCorrespondingToUri(textDocumentItem.getUri()).orElseThrow().completion;
    }
}
