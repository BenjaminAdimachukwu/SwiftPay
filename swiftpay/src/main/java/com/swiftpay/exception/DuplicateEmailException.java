package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to create a customer with an email that already exists.
 *
 * HTTP Status: 409 CONFLICT
 */
public class DuplicateEmailException extends BusinessException {

    private static final String ERROR_CODE = "DUPLICATE_EMAIL";

    public DuplicateEmailException(String email) {
        super(
                "Email already exists: " + email,
                ERROR_CODE,
                HttpStatus.CONFLICT
        );
    }
}