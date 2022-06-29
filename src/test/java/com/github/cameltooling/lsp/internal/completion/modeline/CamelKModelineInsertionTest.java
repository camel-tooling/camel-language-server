package com.github.cameltooling.lsp.internal.completion.modeline;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.annotation.processing.Completion;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelKModelineInsertionTest extends AbstractCamelLanguageServerTest {

    //Use parameterized tests on future
    @Test
    void testProvideInsertionOnEmptyXMLFile() throws Exception {
        FileType type = FileType.XML;
        String contents = "";

        List<CompletionItem> completionItems = getCompletionsFor(type, contents);

        assertThat(completionItems).hasSize(1);
        checkInsertionCompletionAvailableForType(completionItems, type);
    }

    //Why no default args
    List<CompletionItem> getCompletionsFor(FileType type, String contents) throws Exception{
        return getCompletionsFor(type, contents, new Position(0, 0));
    }
    List<CompletionItem>  getCompletionsFor(FileType type, String contents, Position position) throws Exception {
        CamelLanguageServer camelLanguageServer = initializeLanguageServer(contents, type.extension);

        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, position);

        return completions.get().getLeft();
    }

    private void checkInsertionCompletionAvailableForType(List<CompletionItem> completionItems, FileType toAssert) {
        assertThat(completionItems).contains(toAssert.completion);
    }

    private enum FileType {
        XML(".camelk.xml", "<!-- camel-k: -->", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
        Java(".java", "// camel-k:", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
        YAML(".camelk.yaml", "# camel-k", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html");
        //YAML has the special condition `# yaml-language-server: $schema=<urlToCamelKyaml>`

        public final String extension;
        public final CompletionItem completion;


        private FileType(String fileExtension, String expectedLabel, String expectedDocumentation) {
            this.extension = fileExtension;
            this.completion = getCompletion(expectedLabel, expectedDocumentation);
        }

        private static CompletionItem getCompletion(String label, String documentation) {
            CompletionItem completion = new CompletionItem(label);
            completion.setDocumentation(documentation);

            return completion;
        }

    }
}
