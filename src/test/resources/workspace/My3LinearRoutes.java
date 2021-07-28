import org.apache.camel.builder.RouteBuilder;

public class My3LinearRoutes extends RouteBuilder {

    public void configure() {

        from("timer:java?period=1000")
        	.routeId("java1")
        	.log("aKey: {{aKey}} aSecondKey: {{aSecondKey}}")
        	.to("log:info");
        
        from("timer:java?period=1000")
    		.routeId("java2")
    		.log("aKey: {{aKey}} aSecondKey: {{aSecondKey}}")
    		.to("log:info");
        
        from("timer:java?period=1000")
    		.routeId("java3")
    		.log("aKey: {{aKey}} aSecondKey: {{aSecondKey}}")
    		.to("log:info");
    }
}
