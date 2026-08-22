package com.hermes.delivery.route;

import com.hermes.delivery.Delivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeliveryRequestQueueHandler {

    private static final Logger log = LoggerFactory.getLogger(DeliveryRequestQueueHandler.class);

    public void handle(Delivery delivery) {
        log.info("Processing delivery request: id={}, status={}, senderId={}, recipientId={}",
                delivery.getId(),
                delivery.getStatus(),
                delivery.getSenderId(),
                delivery.getRecipientId());

        // later: push to WebSocket topic, update in-memory cache, etc.
    }
}