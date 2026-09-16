package com.moses.dse_track.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // A @Valid @RequestBody failed bean validation (@NotNull, @Size, etc.) —
    // without this, Spring's own default handling still returns 400, but with
    // a generic body that drops every one of the DTOs' field-specific messages.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Validation failed";
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "message", message,
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    // Expected, developer-authored error conditions (not found, validation,
    // ownership, etc.) — the message is always safe to hand back to the client.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "status", 400,
                        "message", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    // Database constraint violations carry raw SQL/column details in their
    // message — never forward those to the client.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "status", 409,
                        "message", "A data conflict occurred. The resource may already exist.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    // A row (e.g. a refresh token) was concurrently modified between read and
    // write — most commonly two near-simultaneous /auth/refresh calls racing
    // on the same token. The loser should retry rather than silently succeed.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "status", 409,
                        "message", "This request was already processed elsewhere — please try again or log in again.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    // Catch-all for anything unexpected (NPE, ArithmeticException, malformed
    // request bodies, etc). These messages can contain internal field/method
    // names, so they're logged server-side only — never returned to the client.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(RuntimeException ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "message", "An unexpected error occurred. Please try again later.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}
