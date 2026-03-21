package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a transaction violates business rules.
 *
 * Examples:
 * - Same source and destination account
 * - Amount is zero or negative
 * - Currency mismatch
 * - Transaction type not allowed for account type
 *
 * HTTP Status: 400 BAD REQUEST
 */
public class InvalidTransactionException extends BusinessException {

    private static final String ERROR_CODE = "INVALID_TRANSACTION";

    public InvalidTransactionException(String reason) {
        super(
                "Invalid transaction: " + reason,
                ERROR_CODE,
                HttpStatus.BAD_REQUEST
        );
    }
}