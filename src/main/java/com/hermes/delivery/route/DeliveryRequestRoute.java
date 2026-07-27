package com.hermes.delivery.route;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class DeliveryRequestRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("seda:delivery-requests?concurrentConsumers=2&size=1000")
                .routeId("delivery-requests-consumer")
                .log("Delivery request entered queue: id=${body.id}, status=${body.status}")
                .to("bean:deliveryRequestQueueHandler?method=handle")
                .log("Delivery request handed off to handler: id=${body.id}");
    }
}