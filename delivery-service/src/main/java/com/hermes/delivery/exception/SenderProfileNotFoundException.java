package com.hermes.delivery.exception;

public class SenderProfileNotFoundException extends RuntimeException {
    public SenderProfileNotFoundException(Long senderProfileId) {
        super("Sender profile not found: " + senderProfileId);
    }
}
