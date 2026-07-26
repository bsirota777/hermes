package com.hermes.integration.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class WireTapRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("file:_INPUT-TAP?noop=true")
                .wireTap("direct:audit")
                .choice()
                .when(simple("${bodyAs(String)} contains 'ERROR'"))
                .log("Routing to ERRORS")
                .to("file:_ERRORS")
                .otherwise()
                .log("Routing to OUTPUT")
                .to("file:_OUTPUT-TAP")
                .end();

        from("direct:audit")
                .routeId("auditRoute")
                .log("AUDIT COPY: ${body}")
                .setHeader("CamelFileName", simple("audit-${exchangeId}.txt"))
                .to("file:_AUDIT-LOG");
    }
}
