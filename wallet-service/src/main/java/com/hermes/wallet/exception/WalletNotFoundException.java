package com.hermes.wallet.exception;

// Not currently thrown: WalletService lazily creates a wallet on first access
// (getOrCreateWalletByUserId) rather than requiring one to exist beforehand.
// Kept for a future strict-lookup path if one is added.
public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(Long userId) {
        super("No wallet found for user " + userId);
    }
}
