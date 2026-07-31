package com.hermes.payment.exception;

import com.stripe.exception.StripeException;

public class OnboardingFailedException extends RuntimeException {
    public OnboardingFailedException(Long userId, StripeException cause) {
        super("Stripe onboarding failed for user " + userId + ": " + cause.getMessage(), cause);
    }
}