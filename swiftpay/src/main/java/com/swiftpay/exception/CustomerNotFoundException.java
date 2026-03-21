package com.swiftpay.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Thrown when a customer is not found by ID or email.
 *
 * HTTP Status: 404 NOT FOUND
 */
public class CustomerNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "CUSTOMER_NOT_FOUND";

    public CustomerNotFoundException(UUID customerId) {
        super(
                "Customer not found with ID: " + customerId,
                ERROR_CODE,
                HttpStatus.NOT_FOUND
        );
    }

    public CustomerNotFoundException(String email) {
        super(
                "Customer not found with email: " + email,
                ERROR_CODE,
                HttpStatus.NOT_FOUND
        );
    }
}