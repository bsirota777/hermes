package com.hermes.user.exception;

public class DriverProfileAlreadyExistsException extends RuntimeException {
    public DriverProfileAlreadyExistsException(Long userId) {
        super("User " + userId + " is already registered as a driver");
    }
}