package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Thrown when an account is not found.
 *
 * HTTP Status: 404 NOT FOUND
 */
public class AccountNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "ACCOUNT_NOT_FOUND";

    public AccountNotFoundException(UUID accountId) {
        super(
                "Account not found with ID: " + accountId,
                ERROR_CODE,
                HttpStatus.NOT_FOUND
        );
    }

    public AccountNotFoundException(String accountNumber) {
        super(
                "Account not found with account number: " + accountNumber,
                ERROR_CODE,
                HttpStatus.NOT_FOUND
        );
    }
}