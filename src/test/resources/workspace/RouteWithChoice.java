import org.apache.camel.builder.RouteBuilder;

public class RouteWithChoice extends RouteBuilder {

    public void configure() {
		from("jms:incomingOrders")
			.choice()
				.when(header("CamelFileName").endsWith(".xml"))
					.to("jms:xmlOrders")
				.when(header("CamelFileName").regex("^.*(csv|csl)$"))
					.to("jms:csvOrders")
				.otherwise()
					.to("jms:badOrders");
    }
}
