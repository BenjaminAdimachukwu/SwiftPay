package com.swiftpay.exception;

import com.swiftpay.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all REST controllers.
 *
 * Catches exceptions thrown anywhere in the application and converts them
 * to proper HTTP responses with consistent error format.
 *
 * @RestControllerAdvice applies this handler to ALL @RestController classes.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========================================
    // Business Exception Handlers
    // ========================================

    /**
     * Handles all BusinessException subclasses.
     *
     * This is a catch-all for our custom business exceptions:
     * - CustomerNotFoundException
     * - InsufficientBalanceException
     * - DuplicateEmailException
     * - AccountFrozenException
     * - etc.
     *
     * Each exception already contains:
     * - errorCode (e.g., "CUSTOMER_NOT_FOUND")
     * - message (e.g., "Customer not found with ID: abc-123")
     * - httpStatus (e.g., HttpStatus.NOT_FOUND)
     *
     * We just need to convert it to ErrorResponse.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            WebRequest request) {

        log.warn("Business exception occurred: {} - {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                ex.getErrorCode(),
                ex.getMessage(),
                getPath(request)
        );

        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(errorResponse);
    }

    // ========================================
    // Validation Exception Handlers
    // ========================================

    /**
     * Handles validation errors from @Valid annotation.
     *
     * Thrown when request DTO fails validation:
     * - @NotBlank, @Email, @Size, @Pattern, etc.
     *
     * Example:
     * POST /api/customers
     * {
     *   "email": "not-an-email",  // ← Fails @Email validation
     *   "firstName": ""            // ← Fails @NotBlank validation
     * }
     *
     * Returns HTTP 400 with field-specific errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        log.warn("Validation error occurred: {} validation errors",
                ex.getBindingResult().getErrorCount());

        // Extract field errors into a map
        Map<String, String> validationErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            validationErrors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.withValidation(
                "VALIDATION_ERROR",
                "Invalid request data. Please check the provided fields.",
                getPath(request),
                validationErrors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException.
     *
     * Thrown when method receives invalid arguments.
     *
     * Example:
     * - Negative amount
     * - Invalid enum value
     * - Business rule violation
     *
     * Returns HTTP 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                "INVALID_ARGUMENT",
                ex.getMessage(),
                getPath(request)
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // ========================================
    // Generic Exception Handlers
    // ========================================

    /**
     * Handles all unexpected exceptions.
     *
     * This is the safety net for bugs and unexpected errors:
     * - NullPointerException
     * - Database connection errors
     * - Out of memory
     * - Anything we didn't anticipate
     *
     * IMPORTANT:
     * - Never expose internal error details to client (security risk!)
     * - Log full stack trace for debugging
     * - Return generic error message to client
     *
     * Returns HTTP 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {

        // Log the REAL error (with stack trace) for developers
        log.error("Unexpected error occurred", ex);

        // Return GENERIC error to client (don't expose internals!)
        ErrorResponse errorResponse = ErrorResponse.of(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later.",
                getPath(request)
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    // ========================================
    // Specific Exception Handlers (Optional)
    // ========================================

    /**
     * Handles NullPointerException specifically.
     *
     * This is a bug! Log it prominently.
     *
     * In production, this should trigger alerts.
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(
            NullPointerException ex,
            WebRequest request) {

        // This is a BUG - log it as ERROR
        log.error("NullPointerException occurred - THIS IS A BUG!", ex);

        ErrorResponse errorResponse = ErrorResponse.of(
                "INTERNAL_ERROR",
                "An unexpected error occurred. Our team has been notified.",
                getPath(request)
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Extract request path from WebRequest.
     *
     * Example:
     * Input: "uri=/api/customers/abc-123"
     * Output: "/api/customers/abc-123"
     */
    private String getPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.replace("uri=", "");
    }
}