package com.hermes.delivery.exception;

import com.hermes.delivery.DeliveryStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Only covers the handlers that actually exist in delivery-service's own GlobalExceptionHandler now -
// the old monolith's handler covered wallet/user/payment exceptions too, since it was one shared handler
// for the whole app; those now live in their own services' handlers.
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleParcelNotFound_returns404() {
        var response = handler.handleParcelNotFound(new ParcelNotFoundException("parcel not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("parcel not found");
    }

    @Test
    void handleInvalidDelivery_returns400() {
        var response = handler.handleInvalidDelivery(new InvalidDeliveryException("bad delivery"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("bad delivery");
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
        assertThat(response.getBody()).isEqualTo("Delivery 42 has already been reserved by another driver");
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
        assertThat(response.getBody()).isEqualTo("Sender profile not found: 3");
    }

    @Test
    void handleRecipientProfileNotFound_returns404() {
        var response = handler.handleRecipientProfileNotFound(new RecipientProfileNotFoundException(5L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Recipient profile not found: 5");
    }

    @Test
    void handleUserNotFound_returns404() {
        var response = handler.handleUserNotFound(new UserNotFoundException("nobody@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("User not found: nobody@example.com");
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
        assertThat(response.getBody()).isEqualTo("email: must not be blank; password: must be at least 8 characters");
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
    void handleAccessDenied_returns403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("not authorized"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("not authorized");
    }

    @Test
    void handleInvalidQrCode_returns400() {
        InvalidQrCodeException ex = new InvalidQrCodeException(1L);

        ResponseEntity<String> response = handler.handleInvalidQrCode(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(ex.getMessage());
    }

    @Test
    void handleUnexpected_returns500WithGenericMessage() {
        var response = handler.handleUnexpected(new RuntimeException("something broke"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("An unexpected error occurred");
    }

    // NOTE: GeocodingFailedException exists in this package but GlobalExceptionHandler currently
    // has no @ExceptionHandler for it - it falls through to the generic 500 handler above. Flagging
    // this as a real gap rather than silently working around it: worth adding a proper handler
    // (probably 400/502, matching how the old monolith treated it) if this is likely to actually occur.
}
