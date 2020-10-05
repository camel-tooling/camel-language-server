public class PartialRoute extends RouteBuilder {

    public void configure() {
        from("file:src/data?noop=true");
    }
}
