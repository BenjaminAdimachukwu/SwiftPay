package com.swiftpay.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CREDIT_CARD("Credit Card", "Payment via credit card", true, false, 3.5),
    DEBIT_CARD("Debit Card", "Payment via debit card", true, false, 2.5),
    BANK_TRANSFER("Bank Transfer", "Direct bank-to-bank transfer", false, true, 1.0),
    DIGITAL_WALLET("Digital Wallet", "Payment via digital wallet", true, false, 3.0),
    CASH("Cash", "Cash payment", true, false, 0.0),
    CHECK("Check", "Payment via check", false, true, 1.5),
    CRYPTOCURRENCY("Cryptocurrency", "Payment via cryptocurrency", true, false, 2.0),
    BUY_NOW_PAY_LATER("Buy Now Pay Later", "Deferred payment service", true, false, 4.0);

    private final String displayName;
    private final String description;
    private final boolean isInstant;
    private final boolean requiresClearing;
    private final double processingFeePercentage;

    public boolean requiresVerification() {
        return this == CREDIT_CARD || this == DEBIT_CARD || this == CRYPTOCURRENCY;
    }

    public boolean supportsRefunds() {
        return this != CASH && this != CHECK;
    }

    public boolean supportsRecurring() {
        return this == CREDIT_CARD || this == DEBIT_CARD ||
                this == BANK_TRANSFER || this == DIGITAL_WALLET;
    }

    public double calculateProcessingFee(double amount) {
        return amount * (processingFeePercentage / 100);
    }

    public static PaymentMethod fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }

        for (PaymentMethod method : PaymentMethod.values()) {
            if (method.name().equalsIgnoreCase(value) ||
                    method.displayName.equalsIgnoreCase(value)) {
                return method;
            }
        }

        throw new IllegalArgumentException("Invalid payment method: " + value);
    }
}
