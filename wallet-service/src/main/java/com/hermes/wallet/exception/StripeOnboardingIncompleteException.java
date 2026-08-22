package com.hermes.wallet.exception;

public class StripeOnboardingIncompleteException extends RuntimeException {
    public StripeOnboardingIncompleteException(Long userId) {
        super("User " + userId + " has started Stripe onboarding but has not completed it. Payouts are not yet enabled.");
    }
}
