package com.hermes.integration.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

//@Component
public class TimerBasedRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("timer:helloTimer?period=5000")
                .log("Hello from Camel! ${body}")
                .setBody(constant("Hello World"))
                .to("log:output");
    }
}
