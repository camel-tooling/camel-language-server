First Time Setup
--------------
0. Fork and clone the repository
1. Install Eclipse [Oxygen Java EE](http://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/oxygenr)
that will have most needed already installed. Alternately,
you can get the [Eclipse IDE for Java developers](http://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/oxygenr)
and just install Eclipse PDE from marketplace.

2. Once installed use `File > Open Projects from File System...` and
point it `camel-language-server` and Eclipse should automatically
detect the projects and import it properly.

3. If you discover an error on `pom.xml` after import about Tycho, you can use Quick Fix
(Ctrl+1) to install the Tycho maven integration.


Building from command line
----------------------------

1. Install [Apache Maven](https://maven.apache.org/)

2. This command will build the server:
```bash    
    $ mvn clean verify
````