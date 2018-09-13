# First Time Setup

0. Fork and clone the repository
1. Install Eclipse [Oxygen Java EE](http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/oxygenr)
that will have most needed already installed. Alternately,
you can get the [Eclipse IDE for Java developers](http://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/oxygenr)
and just install Eclipse PDE from marketplace.

2. Once installed use `File > Open Projects from File System...` and
point it `camel-language-server` and Eclipse should automatically
detect the projects and import it properly.

3. If you discover an error on `pom.xml` after import about Tycho, you can use Quick Fix
(Ctrl+1) to install the Tycho Maven integration.


# Building from command line

1. Install [Apache Maven](https://maven.apache.org/)

2. This command will build the server:
```bash    
    $ mvn clean verify
````

# How to release

* Ensure pom is using only non-snapshot dependencies
* Modify pom version to use a non-snapshot version
* Provide a PR
* Wait that it is reviewed and merged
* Create a tag
* Push the tag to camel-tooling organization repository
** A build will start automatically on https://travis-ci.org/camel-tooling/camel-language-server
** Ensure build is OK
* Modify pom version to use an incremented snapshot version to prepare next release iteration
* Provide a PR

# Technical Overview

## Dependencies

As this is an implementation of Language Server Protocol for Apache Camel, it is recommended to start by reading:
* information about the Language Server Protocol (LSP):
    * [LSP overview](https://microsoft.github.io/language-server-protocol/overview) (a must)
    * [LSP specification](https://microsoft.github.io/language-server-protocol/specification) (can be useful to search for specific parts when needed)
    * an important point is that all API is based on position (line, column) in a text file, so the LSP for Apache Camel implementation needs to be able to always provide and understand this information
* information about LSP4J
    * LSP4J is a Java implementation of the LSP. This is library used for writing the LSP for Apache Camel
    * it is recommended to read at least the [getting started](https://github.com/eclipse/lsp4j/blob/master/documentation/README.md)
    * LSP4J is using Future, the goal is to provide an API which is completely asynchronous for a better User Experience, see [Future javadoc](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html) and [Vogella doc about CompletableFuture](http://www.vogella.com/tutorials/JavaConcurrency/article.html#completablefuture) and there is also a lot of tutorial on the web
* information about Camel
    * see [official website](http://camel.apache.org/)

## LSP for Apache Camel architecture explanations

[CamelTextDocumentService](src/main/java/com/github/cameltooling/lsp/internal/CamelTextDocumentService.java) is the main entry point currently as the current implementation is dealing with a single XML file. The various methods of the CamelTextDocumentService are called by the LSP clients. When clients are implemented correctly, they are called depending on the capabilities declared in [CamelLanguageServer.createServerCapabilities()](src/main/java/com/github/cameltooling/lsp/internal/CamelLanguageServer.java).

The metamodel from the Camel Catalog describing the Camel components is represented by the java class [ComponentModel](https://github.com/camel-tooling/camel-tooling-common/blob/master/src/main/java/com/github/cameltooling/model/ComponentModel.java).
The model describing the Camel URI is in [com.github.cameltooling.lsp.internal.instancemodel](https://github.com/camel-tooling/camel-language-server/tree/master/src/main/java/com/github/cameltooling/lsp/internal/instancemodel) package. The top-level Camel element is [CamelURIInstance](src/main/java/com/github/cameltooling/lsp/internal/instancemodel/CamelURIInstance.java).
The model describing the Camel URI is storing the position of each sub-model elements (Camel component name, path parameter, Option parameter with its key and value). The naming tries to be as close as possible as the Apache Camel one.
All Camel elements of the model are inheriting from [CamelUriElementInstance](src/main/java/com/github/cameltooling/lsp/internal/instancemodel/CamelUriElementInstance.java).

For instance, with the Camel URI "timer:timerName?delay=10s":
* [CamelURIInstance](src/main/java/com/github/cameltooling/lsp/internal/instancemodel/CamelURIInstance.java) represents "timer:timerName?delay=10s"
* [CamelComponentURIInstance](src/main/java/com/github/cameltooling/lsp/internal/instancemodel/CamelComponentURIInstance.java) represents "timer"
* [PathParamURIInstance](src/main/java/com/github/cameltooling/lsp/internal/instancemodel/PathParamURIInstance.java) represents "timerName"
* [OptionParamURIInstance](src/main/java/com/github/cameltooling/lsp/internal/instancemodel/OptionParamURIInstance.java) represents "delay=10s"
* [OptionParamKeyURIInstance](src/main/java/com/github/cameltooling/lsp/internal/instancemodel/OptionParamKeyURIInstance.java) represents "delay"
* [OptionParamValueURIInstance](src/main/java/com/github/cameltooling/lsp/internal/instancemodel/OptionParamValueURIInstance.java) represents "10s"


