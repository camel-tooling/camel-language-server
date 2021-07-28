import org.apache.camel.builder.RouteBuilder;

public class LinearRoute extends RouteBuilder {

    public void configure() {

        from("timer:java?period=1000")
        	.routeId("java2")
        	.log("aKey: {{aKey}} aSecondKey: {{aSecondKey}}")
        	.to("log:info");
    }
}
