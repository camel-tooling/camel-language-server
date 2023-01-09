package com.github.cameltooling.lsp.internal.parser;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentItem;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class CamelEIPParser {

	private final TextDocumentItem document;

	public CamelEIPParser(TextDocumentItem textDocumentItem) {
		this.document = textDocumentItem;
	}

	public boolean canPutEIP(Position position) {
		Pattern CHOICE_EIP_PATTERN = Pattern.compile("\\)\\s*[.[c[h[o[i[c[e]?]?]?]?]?]?]?\\(?$", Pattern.MULTILINE);
		ParserFileHelperUtil util = new ParserFileHelperUtil();
		String textUntilPosition = util.getTextUntilPosition(document, position);

		return CHOICE_EIP_PATTERN.matcher(textUntilPosition).find();
	}

	public CompletableFuture<List<CompletionItem>> getCompletions() {
		return CompletableFuture.completedFuture(
				List.of(choiceEIPcompletion())
		);
	}

	private CompletionItem choiceEIPcompletion() {
		String newLine = System.getProperty("line.separator");
		CompletionItem completion = new CompletionItem("Content Based Router");
		completion.setDocumentation(
				"Read more: https://camel.apache.org/components/3.11.x/eips/content-based-router-eip.html"
		);
		completion.setInsertText(
				".choice()" + newLine +
						".when()" + newLine +
						".to()" + newLine +
						".when()" + newLine +
						".to()" + newLine +
						".otherwise()" + newLine +
						".to()"
		);

		return completion;
	}
}
