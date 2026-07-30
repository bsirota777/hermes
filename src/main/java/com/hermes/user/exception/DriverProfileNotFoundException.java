package com.hermes.user.exception;

public class DriverProfileNotFoundException extends RuntimeException {
    public DriverProfileNotFoundException(Long userId) {
        super("Driver profile not found for user id: " + userId);
    }
}