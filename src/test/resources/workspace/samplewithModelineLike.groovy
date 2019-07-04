// camel-k: language=groovy

from('direct:greeting-api')
    .to('log:api?showAll=true&multiline=true') 
    .setBody()
        .simple('Hello from ${headers.name}')