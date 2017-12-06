#!/bin/bash
set -e

workdir=`pwd`

mkdir -p ${workdir}/jars

# Download the camel language server from github
curl https://github.com/lhein/camel-language-server/releases/download/untagged-7a9f129c944ab427033a/camel-lsp-server-1.0.0-SNAPSHOT.jar > ${workdir}/jars/language-server.jar
