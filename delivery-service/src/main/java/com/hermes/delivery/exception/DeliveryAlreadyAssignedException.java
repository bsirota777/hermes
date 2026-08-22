package com.hermes.delivery.exception;

public class DeliveryAlreadyAssignedException extends RuntimeException {
    public DeliveryAlreadyAssignedException(Long deliveryId) {
        super("Delivery %d has already been reserved by another driver".formatted(deliveryId));
    }
}