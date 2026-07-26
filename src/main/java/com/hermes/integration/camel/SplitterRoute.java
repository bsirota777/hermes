package com.hermes.integration.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

//@Component
public class SplitterRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("file:_INPUT-SPLIT?noop=true")
                .split(body().tokenize("\n"))
                .log("Processing line: ${body}")
                .setHeader("CamelFileName", simple("fruit-${exchangeProperty.CamelSplitIndex}.txt"))
                .to("file:_OUTPUT-SPLIT")
                .end();
    }
}
