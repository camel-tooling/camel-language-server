// camel-k: language=kotlin

from("timer:kotlin?period=1s")
    .routeId("kotlin")
    .setBody()
        .constant("Hello Camel K!")
    .process().message {
        it.headers["RandomValue"] = rnd.nextInt()
    }
    .to("log:info?showAll=true&multiline=true")