package com.github.cameltooling.lsp.internal.parser;

import com.github.cameltooling.lsp.internal.completion.modeline.CamelKModelineFileType;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.Arrays;
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
        return CamelKModelineFileType.getFileTypeCorrespondingToUri(document.getUri()).isPresent();
    }

    private boolean lineIsEmpty(int line) {
        return new ParserFileHelperUtil().getLine(document, line).isBlank();
    }

    private boolean noModelineInsertedAlready() {
        String modeline = CamelKModelineFileType.getFileTypeCorrespondingToUri(document.getUri()).orElseThrow()
                .modeline;

        return !Arrays.stream(document.getText().split("\n")).anyMatch(line -> line.startsWith(modeline));
    }

    private boolean previousLinesAreCommentsOrEmpty(int line) {
        String textBeforeLine = IntStream.range(0, line).boxed()
                .map(currLine -> new ParserFileHelperUtil().getLine(document,currLine))
                .collect(Collectors.joining("\n"));

        return CamelKModelineFileType.getFileTypeCorrespondingToUri(document.getUri()).orElseThrow()
                .checkTextIsCommentsDelegate.apply(textBeforeLine);
    }
}
