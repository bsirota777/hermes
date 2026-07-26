package com.hermes.integration.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

//@Component
public class FileProcessingRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("file:_INPUT?noop=true")
                .choice()
                .when(simple("${bodyAs(String)} contains 'ERROR'"))
                .log("Routing to ERRORS")
                .to("file:_ERRORS")
                .otherwise()
                .log("Routing to OUTPUT")
                .to("file:_OUTPUT")
                .end();
    }
}
