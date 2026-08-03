package com.hermes.exception;

import com.hermes.delivery.DeliveryStatus;
import com.hermes.delivery.exception.*;
import com.hermes.geocoding.exception.GeocodingFailedException;
import com.hermes.parcel.exception.ParcelNotFoundException;
import com.hermes.payment.exception.OnboardingFailedException;
import com.hermes.payment.exception.PayoutFailedException;
import com.hermes.payment.exception.StripeAccountNotLinkedException;
import com.hermes.payment.exception.StripeOnboardingIncompleteException;
import com.hermes.user.exception.*;
import com.hermes.wallet.exception.InsufficientFundsException;
import com.hermes.wallet.exception.WalletNotFoundException;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEmailExists_returns409() {
        var response = handler.handleEmailExists(new EmailAlreadyExistsException("driver@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Email already exists: driver@example.com");
    }

    @Test
    void handleParcelNotFound_returns404() {
        // TODO: confirm ParcelNotFoundException's actual constructor
        var response = handler.handleParcelNotFound(new ParcelNotFoundException("parcel not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("parcel not found");
    }

    @Test
    void handleInvalidDelivery_returns400() {
        // TODO: confirm InvalidDeliveryException's actual constructor
        var response = handler.handleInvalidDelivery(new InvalidDeliveryException("bad delivery"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("bad delivery");
    }

    @Test
    void handleInsufficientFunds_returns402() {
        var response = handler.handleInsufficientFunds(
                new InsufficientFundsException(1L, new BigDecimal("50.00"), new BigDecimal("20.00")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(response.getBody()).isEqualTo(
                "Wallet 1 has insufficient funds: requested 50.00, available 20.00");
    }

    @Test
    void handleWalletNotFound_returns404() {
        var response = handler.handleWalletNotFound(new WalletNotFoundException(7L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("No wallet found for user 7");
    }

    @Test
    void handleValidationErrors_joinsMultipleFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("object", "email", "must not be blank"),
                new FieldError("object", "password", "must be at least 8 characters")
        ));

        var response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(
                "email: must not be blank; password: must be at least 8 characters");
    }

    @Test
    void handleValidationErrors_withNoFieldErrors_returnsDefaultMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        var response = handler.handleValidationErrors(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Validation failed");
    }

    @Test
    void handleDeliveryNotFound_returns404() {
        var response = handler.handleDeliveryNotFound(new DeliveryNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Delivery not found: 42");
    }

    @Test
    void handleDeliveryAlreadyAssigned_returns409() {
        var response = handler.handleDeliveryAlreadyAssigned(new DeliveryAlreadyAssignedException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(
                "Delivery 42 has already been reserved by another driver");
    }

    @Test
    void handleInvalidStatusTransition_returns409() {
        var response = handler.handleInvalidStatusTransition(
                new InvalidStatusTransitionException(DeliveryStatus.CREATED, DeliveryStatus.DELIVERED));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Cannot transition delivery from CREATED to DELIVERED");
    }

    @Test
    void handleSenderProfileNotFound_returns404() {
        var response = handler.handleSenderProfileNotFound(new SenderProfileNotFoundException(3L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Sender not found: 3");
    }

    @Test
    void handleRecipientProfileNotFound_returns404() {
        var response = handler.handleRecipientProfileNotFound(new RecipientProfileNotFoundException(5L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Recipient not found: 5");
    }

    @Test
    void handleDriverProfileNotFound_returns404() {
        var response = handler.handleDriverProfileNotFound(new DriverProfileNotFoundException(9L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Driver profile not found for user id: 9");
    }

    @Test
    void handleUserNotFound_returns404() {
        var response = handler.handleUserNotFound(new UserNotFoundException(11L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("User not found with id: 11");
    }

    @Test
    void handleStripeAccountNotLinked_returns409() {
        var response = handler.handleStripeAccountNotLinked(new StripeAccountNotLinkedException(14L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(
                "User 14 has not started Stripe onboarding. Start onboarding before cashing out.");
    }

    @Test
    void handleStripeOnboardingIncomplete_returns409() {
        var response = handler.handleStripeOnboardingIncomplete(new StripeOnboardingIncompleteException(14L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(
                "User 14 has started Stripe onboarding but has not completed it. Payouts are not yet enabled.");
    }

    @Test
    void handlePayoutFailed_returns502WithGenericMessage() {
        StripeException stripeException = mock(StripeException.class);
        when(stripeException.getMessage()).thenReturn("card declined");

        var response = handler.handlePayoutFailed(
                new PayoutFailedException("acct_123", new BigDecimal("25.00"), stripeException));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isEqualTo("Payout failed. Please try again later.");
    }

    @Test
    void handleOnboardingFailed_returns502WithGenericMessage() {
        StripeException stripeException = mock(StripeException.class);
        when(stripeException.getMessage()).thenReturn("account creation failed");

        var response = handler.handleOnboardingFailed(new OnboardingFailedException(14L, stripeException));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isEqualTo("Onboarding failed. Please try again later.");
    }

    @Test
    void handleGeocodingFailed_returns400WithGenericMessage() {
        var response = handler.handleGeocodingFailed(
                new GeocodingFailedException("123 Fake St", "address not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(
                "Could not verify the provided address. Please check it and try again.");
    }

    @Test
    void handleAccessDenied_returns403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("not authorized"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("not authorized");
    }

    @Test
    void handleBadCredentials_returns401() {
        var response = handler.handleBadCredentials(new BadCredentialsException("Invalid email or password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid email or password");
    }

    @Test
    void handleUnexpected_returns500WithGenericMessage() {
        var response = handler.handleUnexpected(new RuntimeException("something broke"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("An unexpected error occurred");
    }
}