package com.github.cameltooling.lsp.internal.completion.modeline;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 @author joshiraez
 */
class CamelKModelineInsertionTest extends AbstractCamelLanguageServerTest {

    @Nested
    class EmptyFileTest {
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
        void testProvideInsertionOnEmptyYMLFile() throws Exception {
            String extension = ".camelk.yml";
            String contents = "";
            Position position = beginningOfLastLine(contents);

            CamelLanguageServer camelLanguageServer = initializeLanguageServer(contents, extension);
            CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, position);
            List<CompletionItem> completionItems =  completions.get().getLeft();

            assertThat(completionItems).hasSize(1);
            assertThat(completionItems).contains(FileType.YAML.completion);
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
    }

    @Nested
    class FileWithCommentsTest {

        @Nested
        class XMLTest {
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
        }

        @Nested
        class YAMLTest {
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
        }

        @Nested
        class JavaTest {
            @Test
            void testProvideInsertionOnLineCommentedJavaFile() throws Exception {
                FileType type = FileType.Java;
                String contents = "// Example\n";
                Position position = beginningOfLastLine(contents);

                List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

                assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
            }

            @Test
            void testProvideInsertionOnMultipleLineCommentedJavaFile() throws Exception {
                FileType type = FileType.Java;
                String contents = "// Example\n \n //Example //Example \n //EEExample \n";
                Position position = beginningOfLastLine(contents);

                List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

                assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
            }

            @Test
            void testProvideInsertionOnBlockCommentedJavaFile() throws Exception {
                FileType type = FileType.Java;
                String contents = "/* Example */\n";
                Position position = beginningOfLastLine(contents);

                List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

                assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
            }

            @Test
            void testProvideInsertionOnMultipleBlockCommentedJavaFile() throws Exception {
                FileType type = FileType.Java;
                String contents = "/* Example\n \n Example */ /*Example \n EEExample */ \n";
                Position position = beginningOfLastLine(contents);

                List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

                assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
            }

            @Test
            void testProvideInsertionOnMixCommentedJavaFile() throws Exception {
                FileType type = FileType.Java;
                String contents = "/* Example\n \n Example */ /*Example \n //EEExample */ \n";
                Position position = beginningOfLastLine(contents);

                List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

                assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
            }

            @Test
            void testDontProvideInsertionIfExtraTextJava() throws Exception {
                FileType type = FileType.Java;
                String contents = "// Example\npublic class CamelExample {\n";
                Position position = beginningOfLastLine(contents);

                List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

                assertNoCompletionsAvailable(completionItems);
            }
        }
    }

    @Nested
    class ModelineAlreadyPresentTest {
        @Test
        void testDontProvideInsertionOnXMLFileWithModeline() throws Exception {
            FileType type = FileType.XML;
            String contents = "<!-- camel-k: -->\n";
            Position position = beginningOfLastLine(contents);

            List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

            assertNoCompletionsAvailable(completionItems);
        }

        @Test
        void testDontProvideInsertionOnXMLFileWithModelineAfterCursorPosition() throws Exception {
            FileType type = FileType.XML;
            String contents = "\n<!-- camel-k: -->\n";
            Position position = new Position(0,0);

            List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

            assertNoCompletionsAvailable(completionItems);
        }

        @Test
        void testDontProvideInsertionOnYAMLFileWithModeline() throws Exception {
            FileType type = FileType.YAML;
            String contents = "# camel-k:\n";
            Position position = beginningOfLastLine(contents);

            List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

            assertNoCompletionsAvailable(completionItems);
        }

        @Test
        void testDontProvideInsertionOnYAMLFileWithModelineAfterCursorPosition() throws Exception {
            FileType type = FileType.YAML;
            String contents = "\n# camel-k:\n";
            Position position = new Position(0,0);

            List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

            assertNoCompletionsAvailable(completionItems);
        }

        @Test
        void testDontProvideInsertionOnJavaFileWithModeline() throws Exception {
            FileType type = FileType.Java;
            String contents = "// camel-k:\n";
            Position position = beginningOfLastLine(contents);

            List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

            assertNoCompletionsAvailable(completionItems);
        }

        @Test
        void testDontProvideInsertionOnJavaFileWithModelineAfterCursorPosition() throws Exception {
            FileType type = FileType.Java;
            String contents = "\n// camel-k:\n";
            Position position = new Position(0,0);

            List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

            assertNoCompletionsAvailable(completionItems);
        }
    }


    @Nested
    class MidFilePositionTest{
        @Test
        void testProvideInsertionIfCursorBetweenCommentsAndStartOfCode() throws Exception {
            FileType type = FileType.Java;
            String contents = "// Example\n\npublic class CamelExample {\n";
            Position position = new Position(1,0);

            List<CompletionItem> completionItems = getCompletionsFor(type, contents, position);

            assertCompletionItemsHasExpectedCompletionForType(type, completionItems);
        }
    }

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

    private List<CompletionItem>  getCompletionsFor(FileType type, String contents, Position position) throws Exception {
        CamelLanguageServer camelLanguageServer = initializeLanguageServer(contents, type.extension);

        CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(camelLanguageServer, position);

        return completions.get().getLeft();
    }

    private void checkInsertionCompletionAvailableForType(List<CompletionItem> completionItems, FileType toAssert) {
        assertThat(completionItems).contains(toAssert.completion);
    }

    private enum FileType {
        XML(".camelk.xml", "<!-- camel-k: -->", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
        Java(".java", "// camel-k: ", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html"),
        YAML(".camelk.yaml", "# camel-k: ", "Read more: https://camel.apache.org/camel-k/1.9.x/cli/modeline.html");

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
