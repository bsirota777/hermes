package com.hermes.delivery.exception;

public class RecipientProfileNotFoundException extends RuntimeException {
    public RecipientProfileNotFoundException(Long recipientProfileId) {
        super("Recipient profile not found: " + recipientProfileId);
    }
}
