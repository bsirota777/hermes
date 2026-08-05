package com.hermes.delivery.exception;

// com.hermes.delivery.exception
public class InvalidQrCodeException extends RuntimeException {
    public InvalidQrCodeException(Long deliveryId) {
        super("Invalid QR code for delivery id: " + deliveryId);
    }
}