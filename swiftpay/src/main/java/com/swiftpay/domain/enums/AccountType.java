package com.swiftpay.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {
    PERSONAL("Personal", "Individual consumer account", 5000.00, 50000.00, true),
    BUSINESS("Business", "Business account", 50000.00, 500000.00, true),
    MERCHANT("Merchant", "Merchant account for receiving payments", 100000.00, 1000000.00, false),
    ESCROW("Escrow", "Holding account for secured transactions", Double.MAX_VALUE, Double.MAX_VALUE, false),
    SYSTEM("System", "Internal system account", Double.MAX_VALUE, Double.MAX_VALUE, true),
    SAVINGS("Savings", "Savings account with interest", 2000.00, 100000.00, true),
    PREMIUM("Premium", "Premium account with enhanced features", 25000.00, 250000.00, true);

    private final String displayName;
    private final String description;
    private final double dailyTransactionLimit;
    private final double accountBalanceLimit;
    private final boolean canInitiatePayments;

    public boolean canReceivePayments() {
        return true;
    }

    public boolean requiresKYC() {
        return this != SYSTEM;
    }

    public boolean isWithinDailyLimit(double amount) {
        return amount <= dailyTransactionLimit;
    }

    public static AccountType fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Account type cannot be null");
        }

        for (AccountType type : AccountType.values()) {
            if (type.name().equalsIgnoreCase(value) ||
                    type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid account type: " + value);
    }

}
