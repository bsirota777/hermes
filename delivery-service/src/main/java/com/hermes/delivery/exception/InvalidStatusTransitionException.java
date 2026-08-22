package com.hermes.delivery.exception;

import com.hermes.delivery.DeliveryStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(DeliveryStatus from, DeliveryStatus to) {
        super("Cannot transition delivery from %s to %s".formatted(from, to));
    }
}