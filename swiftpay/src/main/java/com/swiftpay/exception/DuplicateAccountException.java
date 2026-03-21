package com.swiftpay.exception;

import com.swiftpay.domain.enums.AccountType;
import com.swiftpay.domain.enums.Currency;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Thrown when attempting to create a duplicate account.
 * Customer can only have one account per type+currency combination.
 *
 * HTTP Status: 409 CONFLICT
 */
public class DuplicateAccountException extends BusinessException {

    private static final String ERROR_CODE = "DUPLICATE_ACCOUNT";

    public DuplicateAccountException(UUID customerId, AccountType accountType, Currency currency) {
        super(
                String.format(
                        "Customer %s already has a %s account in %s",
                        customerId, accountType, currency
                ),
                ERROR_CODE,
                HttpStatus.CONFLICT
        );
    }
}