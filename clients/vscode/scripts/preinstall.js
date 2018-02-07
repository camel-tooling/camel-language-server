'use strict';
const fs = require('fs');
const download = require('download')
var url = "https://github.com/lhein/camel-language-server/releases/download/untagged-d42064681113e838bd59/camel-lsp-server-1.0.0-SNAPSHOT.jar"

download(url).then(data => {
    fs.writeFileSync('./jars/language-server.jar', data);
});
