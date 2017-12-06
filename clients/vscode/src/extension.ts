'use strict';

import * as path from 'path';
import { workspace, ExtensionContext, window, StatusBarAlignment, commands, ViewColumn, Uri, CancellationToken, TextDocumentContentProvider, TextEditor, WorkspaceConfiguration, languages, IndentAction } from 'vscode';
import { LanguageClient, LanguageClientOptions, StreamInfo, Position as LSPosition, Location as LSLocation, Protocol2Code, ServerOptions, TransportKind, Executable } from 'vscode-languageclient';

var os = require('os');
var glob = require('glob');
import { StatusNotification,ClassFileContentsRequest,ProjectConfigurationUpdateRequest,MessageType,ActionableNotification,FeatureStatus } from './protocol';

var storagePath;
var oldConfig;

var lastStatus;

export function activate(context: ExtensionContext) {
	// Let's enable Javadoc symbols autocompletion, shamelessly copied from MIT licensed code at
	// https://github.com/Microsoft/vscode/blob/9d611d4dfd5a4a101b5201b8c9e21af97f06e7a7/extensions/typescript/src/typescriptMain.ts#L186
	languages.setLanguageConfiguration('xml', {
		wordPattern: /(-?\d*\.\d\w*)|([^\`\~\!\@\#\%\^\&\*\(\)\-\=\+\[\{\]\}\\\|\;\:\'\"\,\.\<\>\/\?\s]+)/g,
	});

	storagePath = context.storagePath;
	if (!storagePath) {
		storagePath = getTempWorkspace();
	}

	let serverOptions: Executable = {
		command: 'java',
		args: [ "-jar", "jars/language-server.jar" ],
		options: {stdio:"pipe"}
	};

	// Options to control the language client
	let clientOptions: LanguageClientOptions = {
		// Register the server for xml
		documentSelector: ['xml'],
		synchronize: {
			configurationSection: 'xml',
			// Notify the server about file changes to .java files contain in the workspace
			fileEvents: [
				workspace.createFileSystemWatcher('**/*.xml'),
			],
		}
	};

	let item = window.createStatusBarItem(StatusBarAlignment.Right, Number.MIN_VALUE);
    oldConfig = getServerConfiguration();
	// Create the language client and start the client.
	let languageClient = new LanguageClient('xml','Language Support for Apache Camel', serverOptions, clientOptions);
	languageClient.onNotification(StatusNotification.type, (report) => {
		console.log(report.message);
		switch (report.type) {
			case 'Started':
				item.text = '$(thumbsup)';
				lastStatus = item.text;
				break;
			case 'Error':
				item.text = '$(thumbsdown)';
				lastStatus = item.text;
				break;
			case 'Message':
				item.text = report.message;
				setTimeout(()=> {item.text = lastStatus;}, 3000);
				break;
		}
		item.command = 'java.open.output';
		item.tooltip = report.message;
		toggleItem(window.activeTextEditor, item);
	});
	languageClient.onNotification(ActionableNotification.type, (notification) => {
		let show = null;
		switch (notification.severity) {
			case MessageType.Log:
				show = logNotification;
				break;
			case MessageType.Info:
				show = window.showInformationMessage;
				break;
			case MessageType.Warning:
				show = window.showWarningMessage;
				break;
			case MessageType.Error:
				show = window.showErrorMessage;
				break;
		}
		if (!show) {
			return;
		}
		const titles = notification.commands.map(a => a.title);

		show(notification.message, ...titles).then((selection )=>{
			for(let action of notification.commands) {
				if (action.title === selection) {
					let args:any[] = (action.arguments)?action.arguments:[];
					commands.executeCommand(action.command, ...args);
					break;
				}
			}
		});
	});

	commands.registerCommand('java.open.output', ()=>{
		languageClient.outputChannel.show(ViewColumn.Three);
	});

	window.onDidChangeActiveTextEditor((editor) =>{
		toggleItem(editor, item);
	});

	let provider: TextDocumentContentProvider= <TextDocumentContentProvider> {
		onDidChange: null,
		provideTextDocumentContent: (uri: Uri, token: CancellationToken): Thenable<string> => {
			return languageClient.sendRequest(ClassFileContentsRequest.type, { uri: uri.toString() }, token).then((v: string):string => {
				return v || '';
			});
		}
	};
	workspace.registerTextDocumentContentProvider('xml', provider);

	item.text = 'Starting Toulouse Language Server...';
	toggleItem(window.activeTextEditor, item);
	let disposable = languageClient.start();
	// Push the disposable to the context's subscriptions so that the
	// client can be deactivated on extension deactivation
	context.subscriptions.push(disposable);
}

function logNotification(message:string, ...items: string[]) {
	return new Promise((resolve, reject) => {
    	console.log(message);
	});
}

function setIncompleteClasspathSeverity(severity:string) {
	const config = getServerConfiguration();
	const section = 'errors.incompleteClasspath.severity';
	config.update(section, severity, true).then(
		() => console.log(section + ' globally set to '+severity),
		(error) => console.log(error)
	);
}

function projectConfigurationUpdate(languageClient:LanguageClient, uri?: Uri) {
	let resource = uri;
	if (!(resource instanceof Uri)) {
		if (window.activeTextEditor) {
			resource = window.activeTextEditor.document.uri;
		}
	}
}

function setProjectConfigurationUpdate(languageClient:LanguageClient, uri: Uri, status:FeatureStatus) {
	const config = getServerConfiguration();
	const section = 'configuration.updateBuildConfiguration';

	const st = FeatureStatus[status];
	config.update(section, st).then(
		() => console.log(section + ' set to '+st),
		(error) => console.log(error)
	);
	if (status !== FeatureStatus.disabled) {
		projectConfigurationUpdate(languageClient, uri);
	}
}
function toggleItem(editor: TextEditor, item) {
	if(editor && editor.document &&
		(editor.document.languageId === 'xml')){
		item.show();
	} else{
		item.hide();
	}
}

function hasConfigKeyChanged(key, oldConfig, newConfig) {
	return oldConfig.get(key) !== newConfig.get(key);
}

export function parseVMargs(params:any[], vmargsLine:string) {
	if (!vmargsLine) {
		return;
	}
	let vmargs = vmargsLine.match(/(?:[^\s"]+|"[^"]*")+/g);
	if (vmargs === null) {
		return;
	}
	vmargs.forEach (arg => {
		//remove all standalone double quotes
		arg = arg.replace( /(\\)?"/g, function ($0, $1) { return ($1 ? $0 : ''); });
		//unescape all escaped double quotes
		arg = arg.replace( /(\\)"/g, '"');
		if (params.indexOf(arg) < 0) {
			params.push(arg);
		}
	});
}

function getTempWorkspace() {
	return path.resolve(os.tmpdir(),'vscodesws_'+makeRandomHexString(5));
}

function makeRandomHexString(length) {
    var chars = ['0', '1', '2', '3', '4', '5', '6', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'];
    var result = '';
    for (var i = 0; i < length; i++) {
        var idx = Math.floor(chars.length * Math.random());
        result += chars[idx];
    }
    return result;
}

function startedInDebugMode(): boolean {
	let args = (process as any).execArgv;
	if (args) {
		return args.some((arg) => /^--debug=?/.test(arg) || /^--debug-brk=?/.test(arg));
	};
	return false;
}

function getServerConfiguration():WorkspaceConfiguration {
	return workspace.getConfiguration('xml');
}
