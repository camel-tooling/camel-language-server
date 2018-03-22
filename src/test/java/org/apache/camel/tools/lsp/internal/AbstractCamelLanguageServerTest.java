package org.apache.camel.tools.lsp.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;

import com.github.cameltooling.lsp.internal.CamelLanguageServer;

public abstract class AbstractCamelLanguageServerTest {

	protected static final String AHC_DOCUMENTATION = "To call external HTTP services using Async Http Client.";
	protected static final String DUMMY_URI = "dummyUri";
	protected CompletionItem expectedAhcCompletioncompletionItem;

	public AbstractCamelLanguageServerTest() {
		super();
		expectedAhcCompletioncompletionItem = new CompletionItem("ahc:httpUri");
		expectedAhcCompletioncompletionItem.setDocumentation(AHC_DOCUMENTATION);
	}
	
	final class DummyLanguageClient implements LanguageClient {
		@Override
		public void telemetryEvent(Object object) {
		}

		@Override
		public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) {
			return null;
		}

		@Override
		public void showMessage(MessageParams messageParams) {
		}

		@Override
		public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
		}

		@Override
		public void logMessage(MessageParams message) {
		}
	}

	protected CamelLanguageServer initializeLanguageServer(String text)
			throws URISyntaxException, InterruptedException, ExecutionException {
				InitializeParams params = new InitializeParams();
				params.setProcessId(new Random().nextInt());
				params.setRootUri(getTestResource("/workspace/").toURI().toString());
				CamelLanguageServer camelLanguageServer = new CamelLanguageServer();
				camelLanguageServer.connect(new DummyLanguageClient());
				CompletableFuture<InitializeResult> initialize = camelLanguageServer.initialize(params);
				
				assertThat(initialize).isCompleted();
				assertThat(initialize.get().getCapabilities().getCompletionProvider().getResolveProvider()).isTrue();
				
				camelLanguageServer.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(createTestTextDocument(text)));
				
				return camelLanguageServer;
			}

	private TextDocumentItem createTestTextDocument(String text) {
		return new TextDocumentItem(DUMMY_URI, CamelLanguageServer.LANGUAGE_ID, 0, text);
	}

	protected CompletableFuture<Either<List<CompletionItem>, CompletionList>> getCompletionFor(CamelLanguageServer camelLanguageServer, Position position) {
		TextDocumentService textDocumentService = camelLanguageServer.getTextDocumentService();
		
		TextDocumentPositionParams dummyCompletionPositionRequest = new TextDocumentPositionParams(new TextDocumentIdentifier(DUMMY_URI), position);
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> completions = textDocumentService.completion(dummyCompletionPositionRequest);
		return completions;
	}

	public File getTestResource(String name) throws URISyntaxException {
		return Paths.get(CamelLanguageServerTest.class.getResource(name).toURI()).toFile();
	}

}