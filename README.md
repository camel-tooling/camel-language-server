[![Central](https://img.shields.io/maven-central/v/com.github.camel-tooling/camel-lsp-server.svg?style=plastic)]()
[![GitHub tag](https://img.shields.io/github/tag/camel-tooling/camel-language-server.svg?style=plastic)]()
[![Build Status](https://travis-ci.org/camel-tooling/camel-language-server.svg?branch=master)](https://travis-ci.org/camel-tooling/camel-language-server)
[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=camel-lsp-server&metric=alert_status)](https://sonarcloud.io/dashboard?id=camel-lsp-server)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)]()
[![Gitter](https://img.shields.io/gitter/room/camel-tooling/Lobby.js.svg)](https://gitter.im/camel-tooling/Lobby)

# camel-language-server

camel-language-server is a server implementation that provides Camel DSL smartness.
The server adheres to the [language server protocol](https://github.com/Microsoft/language-server-protocol)
and can be used with any editor that supports the protocol. The server utilizes [Apache Camel](https://camel.apache.org/).

# Clients

These clients are available:
* [Eclipse IDE](https://github.com/camel-tooling/camel-lsp-client-eclipse)
* [VS Code](https://github.com/camel-tooling/camel-lsp-client-vscode)
* [Eclipse Che](https://github.com/eclipse/che/pull/8648)
* [Atom Editor](https://github.com/camel-tooling/camel-lsp-client-atom)

Manual configuration is possible with:
* [Sublime Text](https://github.com/camel-tooling/camel-lsp-client-sublime)

Help is welcome to provide more client implementations, especially for:
* [Emacs](https://github.com/camel-tooling/camel-lsp-client-emacs)
* [Eclipse Theia](https://github.com/camel-tooling/camel-lsp-client-theia)

## Features

### Code completion for Camel URI with XML DSL
* Completion for Camel components ![Completion for Camel components](./images/completionComponent.png "Completion for Camel components")
* Completion for Camel attributes ![Completion for Camel attributes](./images/completionAttribute.png "Completion for Camel attributes")
* Completion for Camel attributes value ![Completion for Camel attributes value](./images/completionAttributeValueForChoice.png "Completion for Camel attributes value")

### Hover on Camel URI with XML DSL

On hover, the documentation of the Camel component is available.
![Hover on Camel components](./images/hoverComponent.png "Hover on Camel components")

### Diagnostics (a.k.a. Validation) on Camel URI with XML DSL

On save, diagnostics on Camel URIs are updated:
![Diagnostic on Camel URI](./images/diagnostic.png "Diagnostic on Camel URI")

## Features planned

* As you type reporting of parsing and compilation errors
* More advanced Code completion
* Code outline
* Code navigation
* Code lens (references)
* Highlights
* Code formatting

## Feedback

* File a bug in [GitHub Issues](https://github.com/camel-tooling/camel-language-server/issues).

## License

ASL 2.0, See [LICENSE](LICENSE) file.

