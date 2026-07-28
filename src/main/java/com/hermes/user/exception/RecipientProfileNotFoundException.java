package com.hermes.user.exception;

public class RecipientProfileNotFoundException extends RuntimeException {
    public RecipientProfileNotFoundException(Long recipientId) {
        super("Recipient not found: " + recipientId);
    }
}
