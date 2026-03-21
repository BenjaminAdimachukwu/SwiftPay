package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a transaction with the same idempotency key already exists.
 *
 * NOTE: This might not actually be thrown as an error.
 * Instead, service returns the original transaction.
 *
 * HTTP Status: 409 CONFLICT (if we want to signal it's a duplicate)
 * OR: Just return 200 OK with original transaction
 */
public class DuplicateTransactionException extends BusinessException {

    private static final String ERROR_CODE = "DUPLICATE_TRANSACTION";

    public DuplicateTransactionException(String idempotencyKey) {
        super(
                "Transaction already exists with idempotency key: " + idempotencyKey,
                ERROR_CODE,
                HttpStatus.CONFLICT
        );
    }
}