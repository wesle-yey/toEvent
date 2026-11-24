package com.wesleyey.toEvent.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + Objects.requireNonNullElse(error.getDefaultMessage(), "valor inválido"))
                .toList();
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_ERROR",
                "message", "Os dados enviados são inválidos.",
                "details", details
        ));
    }

    @ExceptionHandler(EventLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleEventLimitExceeded(EventLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "EVENT_LIMIT_EXCEEDED",
                "message", ex.getMessage(),
                "limit", 5
        ));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "USER_ALREADY_EXISTS",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "USER_NOT_FOUND",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "EMAIL_NOT_VERIFIED",
                "message", "Conta não ativada. Por favor, confirme seu e-mail antes de entrar."
        ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "BAD_CREDENTIALS",
                "message", "E-mail ou senha inválidos."
        ));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_TOKEN",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(VerificationEmailCooldownException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationCooldown(VerificationEmailCooldownException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(Map.of(
                        "error", "VERIFICATION_EMAIL_COOLDOWN",
                        "message", ex.getMessage(),
                        "retryAfterSeconds", ex.getRetryAfterSeconds()
                ));
    }

    @ExceptionHandler(EmailQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleEmailQuotaExceeded(EmailQuotaExceededException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "EMAIL_QUOTA_EXCEEDED",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DisposableEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDisposableEmail(DisposableEmailException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "DISPOSABLE_EMAIL",
                "message", ex.getMessage()
        ));
    }
}
