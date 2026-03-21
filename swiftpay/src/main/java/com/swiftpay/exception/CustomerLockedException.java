package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Thrown when attempting to perform operations for a locked customer.
 * Locked usually means security issue (too many failed logins, fraud, etc.).
 *
 * HTTP Status: 403 FORBIDDEN
 */
public class CustomerLockedException extends BusinessException {

    private static final String ERROR_CODE = "CUSTOMER_LOCKED";

    public CustomerLockedException(UUID customerId) {
        super(
                "Customer account is locked: " + customerId,
                ERROR_CODE,
                HttpStatus.FORBIDDEN
        );
    }

    public CustomerLockedException(String email) {
        super(
                "Customer account is locked: " + email + ". Please contact support.",
                ERROR_CODE,
                HttpStatus.FORBIDDEN
        );
    }
}