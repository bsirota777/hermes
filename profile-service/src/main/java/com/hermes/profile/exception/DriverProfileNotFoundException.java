package com.hermes.profile.exception;
public class DriverProfileNotFoundException extends RuntimeException { public DriverProfileNotFoundException(Long userId) { super("Driver profile not found for user " + userId); } }
