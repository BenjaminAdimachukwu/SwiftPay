package com.swiftpay.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum TransactionStatus {
    INITIATED("Initiated", "Transaction initiated", false),
    PENDING("Pending", "Awaiting processing", false),
    PROCESSING("Processing", "Currently processing", false),
    SUCCESS("Success", "Transaction successful", true),
    FAILED("Failed", "Transaction failed", true),
    CANCELLED("Cancelled", "Transaction cancelled", true),
    EXPIRED("Expired", "Transaction expired", true),
    REQUIRES_VERIFICATION("Requires Verification", "Additional verification needed", false),
    ON_HOLD("On Hold", "Transaction on hold for review", false),
    REFUNDED("Refunded", "Transaction refunded", true),
    REVERSING("Reversing", "Transaction being reversed", false),
    REVERSED("Reversed", "Transaction reversed", true);

    private final String displayName;
    private final String description;
    private final boolean isTerminal;

    public boolean isSuccessful() {
        return this == SUCCESS;
    }

    public boolean isFailed() {
        return this == FAILED || this == CANCELLED || this == EXPIRED;
    }

    public boolean isCancellable() {
        return this == INITIATED || this == PENDING || this == REQUIRES_VERIFICATION;
    }

    public boolean isRefundable() {
        return this == SUCCESS;
    }

    public static List<TransactionStatus> getTerminalStatuses() {
        return Arrays.stream(values())
                .filter(status -> status.isTerminal)
                .toList();
    }

    public static List<TransactionStatus> getActiveStatuses() {
        return Arrays.stream(values())
                .filter(status -> !status.isTerminal)
                .toList();
    }

    public static TransactionStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Transaction status cannot be null");
        }

        for (TransactionStatus status : TransactionStatus.values()) {
            if (status.name().equalsIgnoreCase(value) ||
                    status.displayName.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid transaction status: " + value);
    }

}
