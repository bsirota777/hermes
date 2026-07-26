package com.hermes.integration.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

//@Component
public class AggregatorRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("file:_AGGREGATED-INPUT?noop=true")
                .split(body().tokenize("\n"))
                .setBody(simple("${body.toUpperCase()}"))
                .to("direct:collect")
                .end();

        from("direct:collect")
                .aggregate(constant(true), new ConcatAggregationStrategy())
                .completionSize(3)   // combine every 3 messages into one
                .log("Aggregated result: ${body}")
                .to("file:_AGGREGATED-OUTPUT?fileName=summary.txt");
    }

    // Merges each incoming message's body into one combined body
    static class ConcatAggregationStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange; // first message in the group
            }
            String oldBody = oldExchange.getIn().getBody(String.class);
            String newBody = newExchange.getIn().getBody(String.class);
            oldExchange.getIn().setBody(oldBody + "\n" + newBody);
            return oldExchange;
        }
    }
}