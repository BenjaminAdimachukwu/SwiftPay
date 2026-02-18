package com.swiftpay.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionType {
    PAYMENT("Payment", "Customer payment for goods or services"),
    REFUND("Refund", "Return payment to customer"),
    TRANSFER("Transfer", "Transfer funds between accounts"),
    WITHDRAWAL("Withdrawal", "Withdraw funds from account"),
    DEPOSIT("Deposit", "Deposit funds into account"),
    REVERSAL("Reversal", "Reverse a previous transaction"),
    CHARGEBACK("Chargeback", "Disputed transaction reversal");

    private final String displayName;
    private final String description;

    public static TransactionType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }

        for (TransactionType type : TransactionType.values()) {
            if (type.name().equalsIgnoreCase(value) ||
                    type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid transaction type: " + value);
    }
}
