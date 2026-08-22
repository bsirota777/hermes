package com.hermes.profile.exception;
public class DriverProfileAlreadyExistsException extends RuntimeException { public DriverProfileAlreadyExistsException(Long userId) { super("Driver profile already exists for user " + userId); } }
