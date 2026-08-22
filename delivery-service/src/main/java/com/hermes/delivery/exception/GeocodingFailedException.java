package com.hermes.delivery.exception;

public class GeocodingFailedException extends RuntimeException {
    public GeocodingFailedException(String address, String reason) {
        super("Failed to geocode address '" + address + "': " + reason);
    }
    public GeocodingFailedException(String address, Throwable cause) {
        super("Failed to geocode address '" + address + "': " + cause.getMessage(), cause);
    }
}