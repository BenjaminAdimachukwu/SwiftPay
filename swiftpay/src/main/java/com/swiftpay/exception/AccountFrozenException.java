package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Thrown when attempting to perform operations on a frozen account.
 *
 * HTTP Status: 403 FORBIDDEN
 */
public class AccountFrozenException extends BusinessException {

    private static final String ERROR_CODE = "ACCOUNT_FROZEN";

    public AccountFrozenException(UUID accountId) {
        super(
                "Account is frozen and cannot be used for transactions: " + accountId,
                ERROR_CODE,
                HttpStatus.FORBIDDEN
        );
    }

    public AccountFrozenException(String accountNumber) {
        super(
                "Account is frozen and cannot be used for transactions: " + accountNumber,
                ERROR_CODE,
                HttpStatus.FORBIDDEN
        );
    }
}