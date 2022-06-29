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
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelKModelineInsertionTest extends AbstractCamelLanguageServerTest {

    /* EMPTY FILE TESTS */
    @Test
    void testProvideInsertionOnEmptyXMLFile() throws Exception {
        FileType type = FileType.XML;
        String contents = "";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
    }

    @Test
    void testProvideInsertionOnEmptyJavaFile() throws Exception {
        FileType type = FileType.Java;
        String contents = "";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
    }

    @Test
    void testProvideInsertionOnEmptyYAMLFile() throws Exception {
        FileType type = FileType.YAML;
        String contents = "";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
    }

    @Test
    void testInsertionOnLineWithSpacesOrTabs() throws Exception {
        FileType type = FileType.Java;
        String contents = "\t   \t \t";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
    }
    
    @Test
    void testNoInsertionOnLineWithContents() throws Exception {
        FileType type = FileType.Java;
        String contents = "//example";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertNoCompletionsAvailable(completionItems);
    }

    /* FILE WITH COMMENTS ABOVE LINE TEST */
    /* XML */
    @Test
    void testProvideInsertionOnCommentedXMLFile() throws Exception {
        FileType type = FileType.XML;
        String contents = "<!-- One comment -->\n";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
    }

    @Test
    void testProvideInsertionOnMultipleCommentsXMLFile() throws Exception {
        FileType type = FileType.XML;
        String contents = "<!-- One comment --><!-- Moar comments -->\n";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
    }

    @Test
    void testProvideInsertionOnMultipleCommentsOnMultipleLinesXMLFile() throws Exception {
        FileType type = FileType.XML;
        String contents = "<!-- One comment -->\n \n <!-- Moar comments -->\n";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
    }

    @Test
    void testDontProvideInsertionIfExtraTextXML() throws Exception {
        FileType type = FileType.XML;
        String contents = "<!-- One comment --><!-- Moar comments -->\n<tag></tag>\n";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertNoCompletionsAvailable(completionItems);
    }

    /* YAML */
    @Test
    void testProvideInsertionOnCommentedYAMLFile() throws Exception {
        FileType type = FileType.YAML;
        String contents = "# Example\n";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
    }

    @Test
    void testProvideInsertionOnMultipleCommentsOnMultipleLinesYAMLFile() throws Exception {
        FileType type = FileType.YAML;
        String contents = "# Example\n \n #Example2 ####\n ";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
    }

    @Test
    void testDontProvideInsertionIfExtraTextYAML() throws Exception {
        FileType type = FileType.YAML;
        String contents = "# Example\nexample:\n";
        Position position = beginningOfLastLine(contents);

        List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

        assertNoCompletionsAvailable(completionItems);
    }


    /** UTILS **/

    private Position beginningOfLastLine(String text) {
        int lastLine = (int)text.chars().filter(ch -> ch == '\n').count();

        return new Position(lastLine, 0);
    }

    private void assertNoCompletionsAvailable(List<CompletionItem> completionItems) {
        assertThat(completionItems).hasSize(0);
    }

    private void assertCompletionItemsHasExpectedCompletionForType(FileType type, List<CompletionItem> completionItems) {
        assertThat(completionItems).hasSize(1);
        checkInsertionCompletionAvailableForType(completionItems, type);
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
