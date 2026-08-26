package com.hermes.user.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEmailExists_returns409() {
        var response = handler.handleEmailExists(new EmailAlreadyExistsException("dup@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Email already exists: dup@example.com");
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
    void handleUserNotFound_withId_returns404() {
        var response = handler.handleUserNotFound(new UserNotFoundException(42L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("User not found with id: 42");
    }

    @Test
    void handleUserNotFound_withEmail_returns404() {
        var response = handler.handleUserNotFound(new UserNotFoundException("nobody@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("User not found with email: nobody@example.com");
    }

    @Test
    void handleBadCredentials_returns401() {
        var response = handler.handleBadCredentials(new BadCredentialsException("Invalid email or password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid email or password");
    }

    @Test
    void handleAccessDenied_returns403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("not authorized"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("not authorized");
    }

    @Test
    void handleUnexpected_returns500WithGenericMessage() {
        var response = handler.handleUnexpected(new RuntimeException("something broke"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("An unexpected error occurred");
    }

    // NOTE: UserService.registerAsDriver/updateDriverProfile/getDriverProfile now call
    // ProfileServiceClient, a RestClient-backed HTTP call to profile-service. A 409 (profile
    // already exists) or 404 (profile missing) response from profile-service surfaces here as a
    // RestClientResponseException, not as a domain exception - and this handler has no case for
    // it, so it currently falls through to the generic 500 handler above, losing the original
    // status code. Flagging this as a real gap rather than working around it silently: worth
    // adding a handler that maps RestClientResponseException's status back onto the response
    // if these driver-profile error paths are expected to be exercised via user-service.
}
