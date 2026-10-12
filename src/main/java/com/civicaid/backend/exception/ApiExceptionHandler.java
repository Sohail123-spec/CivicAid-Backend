package com.civicaid.backend.exception;

import io.jsonwebtoken.JwtException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors =
                new LinkedHashMap<>();

        for (FieldError error :
                ex.getBindingResult().getFieldErrors()) {

            fieldErrors.put(
                    error.getField(),
                    error.getDefaultMessage());
        }

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Invalid request",
                fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedRequest(
            HttpMessageNotReadableException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Invalid request body",
                null);
    }

    @ExceptionHandler({
        BadRequestException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ApiError> handleBadRequest(
            RuntimeException ex) {

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                ex.getMessage(),
                null);
    }

    @ExceptionHandler({
        AuthenticationCredentialsNotFoundException.class,
        JwtException.class,
        org.springframework.security.authentication.BadCredentialsException.class
    })
    public ResponseEntity<ApiError> handleAuthentication(
            Exception ex) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_ERROR",
                "Authentication failed",
                null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleForbidden(
            AccessDeniedException ex) {

        return build(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Access denied",
                null);
    }

    @ExceptionHandler({ConflictException.class})
    public ResponseEntity<ApiError> handleConflict(
            RuntimeException ex) {

        return build(
                HttpStatus.CONFLICT,
                "CONFLICT",
                ex.getMessage(),
                null);
    }

    @ExceptionHandler({
        ResourceNotFoundException.class,
        ResponseStatusException.class
    })
    public ResponseEntity<ApiError> handleNotFound(
            RuntimeException ex) {

        return build(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                ex.getMessage() == null
                        ? "Resource not found"
                        : ex.getMessage(),
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex) {

        logger.error(
                "Unhandled exception while processing API request",
                ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Internal server error",
                null);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status,
            String code,
            String message,
            Map<String, String> fieldErrors) {

        return ResponseEntity
                .status(status)
                .body(new ApiError(
                        status.value(),
                        code,
                        message,
                        fieldErrors));
    }
}