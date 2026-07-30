package com.hermes.exception;

import com.hermes.delivery.exception.*;
import com.hermes.user.exception.DriverProfileNotFoundException;
import com.hermes.user.exception.EmailAlreadyExistsException;
import com.hermes.user.exception.RecipientProfileNotFoundException;
import com.hermes.user.exception.SenderProfileNotFoundException;
import com.hermes.wallet.exception.InsufficientFundsException;
import com.hermes.wallet.exception.WalletNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<String> handleEmailExists(EmailAlreadyExistsException ex) {
        log.warn("Registration attempted with existing email: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(ParcelNotFoundException.class)
    public ResponseEntity<String> handleParcelNotFound(ParcelNotFoundException ex) {
        log.warn("Parcel not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidDeliveryException.class)
    public ResponseEntity<String> handleInvalidDelivery(InvalidDeliveryException ex) {
        log.warn("Invalid delivery request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<String> handleInsufficientFunds(InsufficientFundsException ex) {
        log.warn("Insufficient Funds request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(ex.getMessage());
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<String> handleWalletNotFound(WalletNotFoundException ex) {
        log.warn("Wallet Not Found request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message);
    }

    @ExceptionHandler(DeliveryNotFoundException.class)
    public ResponseEntity<String> handleDeliveryNotFound(DeliveryNotFoundException ex) {
        log.warn("Delivery not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(DeliveryAlreadyAssignedException.class)
    public ResponseEntity<String> handleDeliveryAlreadyAssigned(DeliveryAlreadyAssignedException ex) {
        log.warn("Delivery already assigned: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<String> handleInvalidStatusTransition(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(SenderProfileNotFoundException.class)
    public ResponseEntity<String> handleSenderProfileNotFound(SenderProfileNotFoundException ex) {
        log.warn("Sender profile not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(RecipientProfileNotFoundException.class)
    public ResponseEntity<String> handleRecipientProfileNotFound(RecipientProfileNotFoundException ex) {
        log.warn("Recipient profile not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(DriverProfileNotFoundException.class)
    public ResponseEntity<String> handleDriverProfileNotFound(DriverProfileNotFoundException ex) {
        log.warn("Driver profile not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // Catch All
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
    }
}