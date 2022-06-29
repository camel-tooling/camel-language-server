package com.github.cameltooling.lsp.internal.completion.modeline;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelKModelineInsertionTest extends AbstractCamelLanguageServerTest {
    @Test
    void testProvideInsertionOnEmptyXMLFile() throws Exception {
        CamelLanguageServer camelLanguageServer = initializeLanguageServer("");

        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, new Position(0, 0));

        List<CompletionItem> completionItems = completions.get().getLeft();
        assertThat(completionItems).hasSize(1);
//        checkInsertionCompletionAvailable(completionItems);
    }

//    private void checkInsertionCompletionAvailable(List<CompletionItem> completionItems) {
//        CompletionItem traitCompletionItem = new CompletionItem("insertion");
//        traitCompletionItem.setDocumentation("Configure a trait. E.g. \"trait=service.enabled=false\"");
//        assertThat(completionItems).contains(traitCompletionItem);
//    }

}
