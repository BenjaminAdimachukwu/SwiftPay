package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all business-related exceptions.
 *
 * All custom exceptions should extend this class.
 * Provides consistent structure: message, error code, HTTP status.
 */
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    /**
     * Creates a business exception.
     *
     * @param message     Human-readable error message
     * @param errorCode   Machine-readable error code (e.g., "CUSTOMER_NOT_FOUND")
     * @param httpStatus  HTTP status code for API responses
     */
    protected BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    /**
     * Creates a business exception with a cause.
     *
     * @param message     Human-readable error message
     * @param errorCode   Machine-readable error code
     * @param httpStatus  HTTP status code
     * @param cause       The underlying exception that caused this
     */
    protected BusinessException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}