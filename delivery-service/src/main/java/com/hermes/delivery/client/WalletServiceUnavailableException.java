package com.hermes.delivery.client;

public class WalletServiceUnavailableException extends RuntimeException {
    public WalletServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}