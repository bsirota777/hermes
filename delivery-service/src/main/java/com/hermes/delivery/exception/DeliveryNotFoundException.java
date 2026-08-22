package com.hermes.delivery.exception;

public class DeliveryNotFoundException extends RuntimeException {
    public DeliveryNotFoundException(Long deliveryId) {
        super("Delivery not found: " + deliveryId);
    }
}