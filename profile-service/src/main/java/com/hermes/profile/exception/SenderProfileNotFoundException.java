package com.hermes.user.exception;

public class SenderProfileNotFoundException extends RuntimeException {
    public SenderProfileNotFoundException(Long senderId) {
        super("Sender not found: " + senderId);
    }
}
