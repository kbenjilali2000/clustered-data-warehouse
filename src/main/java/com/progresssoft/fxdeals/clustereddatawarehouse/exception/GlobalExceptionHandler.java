package com.progresssoft.fxdeals.clustereddatawarehouse.exception;

import com.progresssoft.fxdeals.clustereddatawarehouse.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 *
 * Handles:
 *  - Validation errors thrown by Bean Validation on controller method arguments
 *  - Malformed JSON / unreadable request bodies
 *  - Business / parsing errors signalled by IllegalArgumentException (e.g. CSV header)
 *  - Any other unhandled exceptions
 *
 * For the FX deals import endpoints, most row-level validation is handled
 * inside the service and returned as part of DealImportResultDto. This handler
 * is mainly for request-level problems and truly unexpected failures.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation errors on controller method arguments.
     *
     * Note: for the FX deals import endpoint, we intentionally removed @Valid
     * on the JSON list parameter to allow row-level error reporting instead
     * of a global 400. This handler remains useful for other endpoints.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed for request",
                request.getRequestURI(),
                validationErrors
        );

        log.warn("Validation failed for request {}: {}", request.getRequestURI(), validationErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles malformed JSON or unreadable HTTP message body.
     * Example: wrong JSON syntax, wrong types, etc.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                         HttpServletRequest request) {

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Malformed JSON request: " + ex.getMostSpecificCause().getMessage(),
                request.getRequestURI(),
                null
        );

        log.warn("Malformed JSON for request {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException used for business / parsing errors
     * at the request level (e.g. invalid CSV header, impossible arguments).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                  HttpServletRequest request) {

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        log.warn("Illegal argument for request {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Catch-all handler for any unexpected exceptions.
     * Should be rare, because most row-level issues are handled inside services.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex,
                                                                   HttpServletRequest request) {

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Unexpected server error: " + ex.getMessage(),
                request.getRequestURI(),
                null
        );

        log.error("Unexpected error for request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
