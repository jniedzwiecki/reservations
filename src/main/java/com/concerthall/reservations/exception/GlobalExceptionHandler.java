package com.concerthall.reservations.exception;

import com.concerthall.reservations.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(final ResourceNotFoundException ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("Resource Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientCapacityException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientCapacityException(final InsufficientCapacityException ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("Insufficient Capacity")
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DuplicateTicketException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTicketException(final DuplicateTicketException ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("Duplicate Ticket")
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserNotRemovableException.class)
    public ResponseEntity<ErrorResponse> handleUserNotRemovableException(final UserNotRemovableException ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("User Not Removable")
                .status(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(VenueAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleVenueAccessDeniedException(final VenueAccessDeniedException ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("Access Denied")
                .status(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidEventStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventStateException(final InvalidEventStateException ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("Invalid Event State")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(final BadCredentialsException ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message("Invalid email or password")
                .error("Authentication Failed")
                .status(HttpStatus.UNAUTHORIZED.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(final UsernameNotFoundException ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("User Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(final MethodArgumentNotValidException ex) {
        final Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            final String fieldName = ((FieldError) error).getField();
            final String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        final Map<String, Object> response = new HashMap<>();
        response.put("message", "Validation failed");
        response.put("errors", errors);
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(final IllegalArgumentException ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message(ex.getMessage())
                .error("Invalid Argument")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(final Exception ex) {
        final ErrorResponse error = ErrorResponse.builder()
                .message("An unexpected error occurred")
                .error(ex.getClass().getSimpleName())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
