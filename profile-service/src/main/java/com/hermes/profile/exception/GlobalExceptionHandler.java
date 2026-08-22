package com.hermes.profile.exception;
import com.hermes.profile.geocoding.GeocodingFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {
 @ExceptionHandler(MethodArgumentNotValidException.class) public ResponseEntity<String> validation(MethodArgumentNotValidException ex){return ResponseEntity.badRequest().body(ex.getBindingResult().getFieldErrors().stream().map(e->e.getField()+": "+e.getDefaultMessage()).reduce((a,b)->a+"; "+b).orElse("Validation failed"));}
 @ExceptionHandler(DriverProfileAlreadyExistsException.class) public ResponseEntity<String> exists(DriverProfileAlreadyExistsException ex){return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());}
 @ExceptionHandler({DriverProfileNotFoundException.class,ProfileNotFoundException.class}) public ResponseEntity<String> notFound(RuntimeException ex){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());}
 @ExceptionHandler(GeocodingFailedException.class) public ResponseEntity<String> geocoding(GeocodingFailedException ex){return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.getMessage());}
 @ExceptionHandler(Exception.class) public ResponseEntity<String> unexpected(Exception ex){return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");}
}
