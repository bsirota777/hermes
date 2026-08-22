package com.hermes.wallet.exception;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long walletId, java.math.BigDecimal requested, java.math.BigDecimal available) {
        super("Wallet %d has insufficient funds: requested %s, available %s"
                .formatted(walletId, requested, available));
    }
}
