package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Thrown when account has insufficient balance for a transaction.
 *
 * HTTP Status: 422 UNPROCESSABLE ENTITY
 */
public class InsufficientBalanceException extends BusinessException {

    private static final String ERROR_CODE = "INSUFFICIENT_BALANCE";

    public InsufficientBalanceException(BigDecimal available, BigDecimal required) {
        super(
                String.format(
                        "Insufficient balance. Available: %s, Required: %s",
                        available, required
                ),
                ERROR_CODE,
                HttpStatus.UNPROCESSABLE_ENTITY
        );
    }

    public InsufficientBalanceException(String accountNumber, BigDecimal available, BigDecimal required) {
        super(
                String.format(
                        "Insufficient balance in account %s. Available: %s, Required: %s",
                        accountNumber, available, required
                ),
                ERROR_CODE,
                HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}