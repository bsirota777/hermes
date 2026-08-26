package com.hermes.profile.exception;

import com.hermes.profile.geocoding.GeocodingFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
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
    void validation_joinsMultipleFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("object", "phoneNumber", "must not be blank"),
                new FieldError("object", "address.streetName", "must not be blank")
        ));

        var response = handler.validation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("phoneNumber: must not be blank; address.streetName: must not be blank");
    }

    @Test
    void validation_withNoFieldErrors_returnsDefaultMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        var response = handler.validation(ex);

        assertThat(response.getBody()).isEqualTo("Validation failed");
    }

    @Test
    void exists_returns409() {
        var response = handler.exists(new DriverProfileAlreadyExistsException(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Driver profile already exists for user 1");
    }

    @Test
    void notFound_handlesDriverProfileNotFoundException() {
        var response = handler.notFound(new DriverProfileNotFoundException(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Driver profile not found for user 1");
    }

    @Test
    void notFound_handlesProfileNotFoundException() {
        var response = handler.notFound(new ProfileNotFoundException(1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("Profile not found for user 1");
    }

    @Test
    void geocoding_returns502() {
        var response = handler.geocoding(new GeocodingFailedException("123 Fake St", "No results found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).contains("123 Fake St").contains("No results found");
    }

    @Test
    void unexpected_returns500WithGenericMessage() {
        var response = handler.unexpected(new RuntimeException("something broke"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("An unexpected error occurred");
    }
}
