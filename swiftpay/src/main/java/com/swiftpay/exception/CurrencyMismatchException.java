package com.swiftpay.exception;

import com.swiftpay.domain.enums.Currency;
import org.springframework.http.HttpStatus;

/**
 * Thrown when attempting to transact between accounts with different currencies.
 *
 * HTTP Status: 400 BAD REQUEST
 */
public class CurrencyMismatchException extends BusinessException {

    private static final String ERROR_CODE = "CURRENCY_MISMATCH";

    public CurrencyMismatchException(Currency sourceCurrency, Currency destinationCurrency) {
        super(
                String.format(
                        "Currency mismatch: source account is %s but destination is %s. " +
                                "Currency conversion is not supported.",
                        sourceCurrency, destinationCurrency
                ),
                ERROR_CODE,
                HttpStatus.BAD_REQUEST
        );
    }
}