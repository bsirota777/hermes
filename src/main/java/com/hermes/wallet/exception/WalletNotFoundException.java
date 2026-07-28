package com.hermes.wallet.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(Long userId) {
        super("No wallet found for user " + userId);
    }
}