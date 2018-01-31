'use strict';
const fs = require('fs');
const download = require('download')
var url = "https://github.com/lhein/camel-language-server/releases/download/untagged-d42064681113e838bd59/vscode-apache-camel-0.0.1.vsix"

download(url).then(data => {
    fs.writeFileSync('./jars/language-server.jar', data);
});
