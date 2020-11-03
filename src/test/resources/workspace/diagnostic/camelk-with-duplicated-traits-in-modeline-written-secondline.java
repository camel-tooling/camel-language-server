// a random comment
// camel-k: trait=quarkus.enabled=true trait=quarkus.enabled=false

public class MyRouteBuilder extends RouteBuilder {

    public void configure() {

        from("file:src/data?noop=true&antExclude=aa&antFilterCaseSensitive=true&runLoggingLevel=OFF")
            .choice()
                .when(xpath("/person/city = 'London'"))
                    .to("file:target/messages/uk")
                .otherwise()
                    .to("file:target/messages/others");
    }

}
