#!/usr/bin/env camel-k

from('direct:greeting-api')
    .to('log:api?showAll=true&multiline=true') 
    .setBody()
        .simple('Hello from ${headers.name}')