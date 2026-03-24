package com.swiftpay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standard error response format for all API errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Machine-readable error code.
     * Examples: CUSTOMER_NOT_FOUND, INSUFFICIENT_BALANCE
     */
    private String errorCode;

    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * When the error occurred.
     */
    private LocalDateTime timestamp;

    /**
     * API path that caused the error.
     */
    private String path;

    /**
     * Field-specific validation errors (for form validation).
     *
     * Map of field name → error message.
     * Example: {"email": "Invalid format", "amount": "Must be positive"}
     *
     * Use this for validation errors on specific fields.
     */
    private Map<String, String> validationErrors;

    /**
     * General error details (for non-validation errors).
     *
     * List of error messages.
     * Example: ["Transaction timed out", "Retry recommended"]
     *
     * Use this for additional context or multiple sub-errors.
     */
    private List<String> details;

    // -------------------------
    // Static Factory Methods
    // -------------------------

    /**
     * Simple error (most common).
     */
    public static ErrorResponse of(String errorCode, String message, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    /**
     * Error with validation errors (field-specific).
     */
    public static ErrorResponse withValidation(String errorCode, String message,
                                               String path, Map<String, String> validationErrors) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .validationErrors(validationErrors)
                .build();
    }

    /**
     * Error with general details.
     */
    public static ErrorResponse withDetails(String errorCode, String message,
                                            String path, List<String> details) {
        return ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .details(details)
                .build();
    }
}