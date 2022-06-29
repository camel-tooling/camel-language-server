package com.github.cameltooling.lsp.internal.parser;

import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineInsertionProcessor;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

public class CamelKModelineInsertionParser {

    public boolean canPutCamelKModeline(Position position, TextDocumentItem textDocumentItem) {
        int currentLine = position.getLine();

        //Save the lines to not have to access multiple times to file?
        return isCamelKFile(textDocumentItem)
                && lineIsEmpty(currentLine, textDocumentItem)
                && noModelineInsertedAlready(textDocumentItem)
                && previousLinesAreCommentsOrEmpty(currentLine,textDocumentItem);
    }

    private boolean isCamelKFile(TextDocumentItem document) {
        return FileType.getFileTypeCorrespondingToUri(document.getUri()).isPresent();
    }

    private boolean lineIsEmpty(int line, TextDocumentItem textDocumentItem) {
        return new ParserFileHelperUtil().getLine(textDocumentItem, line).isEmpty();
    }

    private boolean noModelineInsertedAlready(TextDocumentItem textDocumentItem) {
        //Will do later
        return true;
    }

    private boolean previousLinesAreCommentsOrEmpty(int line, TextDocumentItem textDocumentItem) {
        //Harder than it looks. We have to take into account multi and single comment lines. Will be done when I get to that test
        return true;
    }

    //Unify this somewhere for modeline parsing/completion?
    private enum FileType {
        XML(".camelk.xml", "<!-- camel-k:"),
        Java(".java", "// camel-k:"),
        YAML(".camelk.yaml", "# camel-k");
        //YAML has the special condition `# yaml-language-server: $schema=<urlToCamelKyaml>`

        public final String extension;
        public final String modelineLabel;

        private FileType(String extension, String modelineLabel) {
            this.extension = extension;
            this.modelineLabel = modelineLabel;
        }

        private static Optional<FileType> getFileTypeCorrespondingToUri(String uri) {
            return Arrays.asList(FileType.values()).stream()
                    .filter(type -> uri.endsWith(type.extension))
                    .findFirst();
        }
    }
}
