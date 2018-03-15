'use strict';
const fs = require('fs');
const download = require('download')
var url = "https://github.com/apupier/camel-language-server/releases/download/untagged-5a182c0119681071de73/camel-lsp-server-1.0.0-SNAPSHOT.jar"

download(url).then(data => {
    fs.writeFileSync('./jars/language-server.jar', data);
});
