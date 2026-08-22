package com.hermes.wallet.exception;

public class StripeAccountNotLinkedException extends RuntimeException {
    public StripeAccountNotLinkedException(Long userId) {
        super("User " + userId + " has not started Stripe onboarding. Start onboarding before cashing out.");
    }
}
