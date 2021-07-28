import org.apache.camel.builder.RouteBuilder;

public class RoutesWithChoice extends RouteBuilder {

    public void configure() {
		from("jms:incomingOrders")
			.routeId("route1")
			.choice()
				.when(header("CamelFileName").endsWith(".xml"))
					.to("jms:xmlOrders")
				.when(header("CamelFileName").regex("^.*(csv|csl)$"))
					.to("jms:csvOrders")
				.otherwise()
					.to("jms:badOrders");
		
		from("jms:incomingOrders")
			.routeId("route2")
			.choice()
				.when(header("CamelFileName").endsWith(".xml"))
					.to("jms:xmlOrders")
				.when(header("CamelFileName").regex("^.*(csv|csl)$"))
					.to("jms:csvOrders")
				.otherwise()
					.to("jms:badOrders");
    }
}
