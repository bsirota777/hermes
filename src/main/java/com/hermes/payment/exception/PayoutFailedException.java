package com.hermes.payment.exception;

import com.stripe.exception.StripeException;

import java.math.BigDecimal;

public class PayoutFailedException extends RuntimeException {
    public PayoutFailedException(String stripeAccountId, BigDecimal amount, StripeException cause) {
        super("Payout of " + amount + " to Stripe account " + stripeAccountId + " failed: " + cause.getMessage(), cause);
    }
}