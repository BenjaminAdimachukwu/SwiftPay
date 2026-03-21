package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Thrown when a transaction is not found.
 *
 * HTTP Status: 404 NOT FOUND
 */
public class TransactionNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "TRANSACTION_NOT_FOUND";

    public TransactionNotFoundException(UUID transactionId) {
        super(
                "Transaction not found with ID: " + transactionId,
                ERROR_CODE,
                HttpStatus.NOT_FOUND
        );
    }

    public TransactionNotFoundException(String transactionReference) {
        super(
                "Transaction not found with reference: " + transactionReference,
                ERROR_CODE,
                HttpStatus.NOT_FOUND
        );
    }
}