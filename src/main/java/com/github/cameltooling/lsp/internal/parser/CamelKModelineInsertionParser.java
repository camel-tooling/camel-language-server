package com.github.cameltooling.lsp.internal.parser;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CamelKModelineInsertionParser {

    private final TextDocumentItem document;

    public CamelKModelineInsertionParser(TextDocumentItem document) {
        this.document = document;
    }

    public boolean canPutCamelKModeline(Position position) {
        int currentLine = position.getLine();

        return isCamelKFile()
                && lineIsEmpty(currentLine)
                && noModelineInsertedAlready()
                && previousLinesAreCommentsOrEmpty(currentLine);
    }

    private boolean isCamelKFile() {
        return FileType.getFileTypeCorrespondingToUri(document.getUri()).isPresent();
    }

    private boolean lineIsEmpty(int line) {
        return new ParserFileHelperUtil().getLine(document, line).isBlank();
    }

    private boolean noModelineInsertedAlready() {
        return true;
    }

    private boolean previousLinesAreCommentsOrEmpty(int line) {
        String textBeforeLine = IntStream.range(0, line).boxed()
                .map(currLine -> new ParserFileHelperUtil().getLine(document,currLine))
                .collect(Collectors.joining("\n"));

        return FileType.getFileTypeCorrespondingToUri(document.getUri()).orElseThrow()
                .checkTextIsCommentsDelegate.apply(textBeforeLine);
    }

    private enum FileType {
        XML(".camelk.xml", "<!-- camel-k:", FileType::textIsFullyCommentedXML),
        Java(".java", "// camel-k:", FileType::textIsFullyCommentedJava),
        YAML(".camelk.yaml", "# camel-k", FileType::textIsFullyCommentedYAML);

        public final String extension;
        public final String modelineLabel;
        public final Function<String, Boolean> checkTextIsCommentsDelegate;

        private FileType(String extension, String modelineLabel, Function<String, Boolean> checkTextIsCommentsDelegate) {
            this.extension = extension;
            this.modelineLabel = modelineLabel;
            this.checkTextIsCommentsDelegate = checkTextIsCommentsDelegate;
        }

        private static Optional<FileType> getFileTypeCorrespondingToUri(String uri) {
            return Arrays.asList(FileType.values()).stream()
                    .filter(type -> uri.endsWith(type.extension))
                    .findFirst();
        }

        private static boolean textIsFullyCommentedXML(String text){
            //Remove all segments between <!-- and -->. Check if it's empty.
            Pattern commentRegex = Pattern.compile("<!--.*-->");

            return textIsFullOfRegex(text, commentRegex);
        }

        private static boolean textIsFullyCommentedYAML(String text){
            //Remove all segments between # and \n
            Pattern commentRegex = Pattern.compile("#.*\\n");

            return textIsFullOfRegex(text, commentRegex);
        }

        private static boolean textIsFullyCommentedJava(String text){
            //Line Comments: Remove from // to \n
            //Block Comments: Remove from /* to */. Newlines have to be explicitly added
            Pattern lineComment = Pattern.compile("\\/\\/.*\\n");
            Pattern blockComment = Pattern.compile("\\/\\*(.|\\n)*\\*\\/");
            Pattern commentRegex = Pattern.compile(String.format("(%s|%s)",lineComment.pattern(), blockComment.pattern()));

            return textIsFullOfRegex(text, commentRegex);
        }

        private static boolean textIsFullOfRegex(String text, Pattern regex) {
            //Add an extra carriage return at the end for correct matching with line comments
            String textWithExtraCarriageReturn = text + "\n";
            String textWithoutComments = textWithExtraCarriageReturn.replaceAll(regex.pattern(), "");

            return textWithoutComments.isBlank();
        }
    }
}
