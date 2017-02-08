[![Build Status](https://travis-ci.org/lhein/camel-language-server.svg?branch=master)](https://travis-ci.org/lhein/camel-language-server)

This repository contains only the server implementation for now.
=========================

camel-language-server
=====================

camel-language-server is a server implementation that provides Camel DSL smartness.
The server adheres to the [language server protocol](https://github.com/Microsoft/language-server-protocol)
and can be used with any editor that supports the protocol.  The server utilizes [Apache Camel](http://camel.apache.org/) and [M2Eeclipse](http://www.eclipse.org/m2e/).

Features
--------------
* As you type reporting of parsing and compilation errors
* Code completion
* Javadoc hovers
* Code outline
* Code navigation
* Code lens (references)
* Highlights
* Code formatting


First Time Setup
--------------
0. Fork and clone the repository
1. Install Eclipse [Neon Java EE](http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/neonr)
that will have most needed already installed. Alternately,
you can get the [Eclipse IDE for Java developers](http://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/neonr)
and just install Eclipse PDE from marketplace.

2. Once installed use `File > Open Projects from File System...` and
point it `camel-language-server` and Eclipse should automatically
detect the projects and import it properly.

3. If you discover an error on `pom.xml` after import about Tycho, you can use Quick Fix
(Ctrl+1) to install the Tycho maven integration.


Building from command line
----------------------------

1. Install [Apache Maven](https://maven.apache.org/)

2. This command will build the server into `/org.apache.camel.tools.lsp.product/target/repository` folder:
```bash    
    $ mvn clean verify
````


Feedback
---------

* File a bug in [GitHub Issues](https://github.com/lhein/camel-language-server/issues).
* [Tweet](https://twitter.com/lhein77) us with other feedback.


License
-------
ASL 2.0, See [LICENSE](LICENSE) file.

