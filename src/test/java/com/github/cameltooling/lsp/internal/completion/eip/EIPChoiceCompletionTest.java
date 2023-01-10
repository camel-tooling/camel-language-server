package com.github.cameltooling.lsp.internal.completion.eip;

import com.github.cameltooling.lsp.internal.AbstractCamelLanguageServerTest;
import com.github.cameltooling.lsp.internal.CamelLanguageServer;
import com.github.cameltooling.lsp.internal.util.RouteTextBuilder;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


public class EIPChoiceCompletionTest extends AbstractCamelLanguageServerTest {

	private static final String FROM_ROUTE = "from(\"timer:foo?period={{timer.period}}\")";
	private static final String FROM_ROUTE_WITH_LINE_BREAKS_AND_TABS
			= "from(\"timer:foo?period={{timer.period}}\")\n\t.bean()\n";

	private static final String FROM_ROUTE_WITH_CHOICE_EIP_MID_WRITTEN
			= "from(\"timer:foo?period={{timer.period}}\")\n\t.ch\n";


	@Test
	void testProvideInsertionOnEmptyJavaFile() throws Exception {
		String contents = "";
		Position position = new Position(0,0);

		List<CompletionItem> completionItems = getCompletionsFor(contents, position);
		completionItems = completionItems.stream().filter(
				completionItem -> completionItem.getLabel().startsWith("Content Based Router")
		).collect(Collectors.toList());

		assertThat(completionItems).hasSize(0); //Removing camel-k modeline insertion. Refactor later
	}

	@Test
	void testProvideInsertionOnEmptyJavaClass() throws Exception {
		RouteTextBuilder.DocumentContentWithPosition document =
				RouteTextBuilder.createJavaDocumentClass("");

		List<CompletionItem> completionItems = getCompletionsFor(document.content, document.position);
		completionItems = completionItems.stream().filter(
				completionItem -> completionItem.getLabel().startsWith("Content Based Router")
		).collect(Collectors.toList());


		assertThat(completionItems).hasSize(0);
	}

	@Test
	void testProvideInsertionOnEmptyJavaCamelRoute() throws Exception {
		RouteTextBuilder.DocumentContentWithPosition document =
				RouteTextBuilder.createJavaDocumentCamelRoute("");

		List<CompletionItem> completionItems = getCompletionsFor(document.content, document.position);
		completionItems = completionItems.stream().filter(
				completionItem -> completionItem.getLabel().startsWith("Content Based Router")
		).collect(Collectors.toList());

		assertThat(completionItems).hasSize(0);
	}

	@Test
	void testProvideInsertionAfterFromOnCamelRoute() throws Exception {
		RouteTextBuilder.DocumentContentWithPosition document =
				RouteTextBuilder.createJavaDocumentCamelRoute(FROM_ROUTE);

		List<CompletionItem> completionItems = getCompletionsFor(document.content, document.position);
		completionItems = completionItems.stream().filter(
				completionItem -> completionItem.getLabel().startsWith("Content Based Router")
		).collect(Collectors.toList());


		assertThat(completionItems).hasSize(1);
	}

	@Test
	void testProvideInsertionAfterFromOnCamelRouteWithLineBreaks() throws Exception {
		RouteTextBuilder.DocumentContentWithPosition document =
				RouteTextBuilder.createJavaDocumentCamelRoute(FROM_ROUTE_WITH_LINE_BREAKS_AND_TABS);

		List<CompletionItem> completionItems = getCompletionsFor(document.content, document.position);
		completionItems = completionItems.stream().filter(
				completionItem -> completionItem.getLabel().startsWith("Content Based Router")
		).collect(Collectors.toList());


		assertThat(completionItems).hasSize(1);
	}

	@Test
	void testProvideInsertionMidWritingChoice() throws Exception {
		RouteTextBuilder.DocumentContentWithPosition document =
				RouteTextBuilder.createJavaDocumentCamelRoute(FROM_ROUTE_WITH_CHOICE_EIP_MID_WRITTEN);

		List<CompletionItem> completionItems = getCompletionsFor(document.content, document.position);
		completionItems = completionItems.stream().filter(
				completionItem -> completionItem.getLabel().startsWith("Content Based Router")
		).collect(Collectors.toList());


		assertThat(completionItems).hasSize(1);
	}


	private List<CompletionItem> getCompletionsFor(String contents, Position position) throws Exception {
		CamelLanguageServer camelLanguageServer = initializeLanguageServer(contents, ".java");

		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = getCompletionFor(
				camelLanguageServer, position);

		return completions.get().getLeft();
	}
}
