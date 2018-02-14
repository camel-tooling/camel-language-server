'use strict';
const fs = require('fs');
const download = require('download')
var url = "https://github.com/lhein/camel-language-server/releases/download/untagged-91e507435585e8ecfacd/camel-lsp-server-1.0.0-SNAPSHOT.jar"

download(url).then(data => {
    fs.writeFileSync('./jars/language-server.jar', data);
});
