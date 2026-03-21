package com.swiftpay.dto.response;

import com.swiftpay.domain.enums.AccountType;
import com.swiftpay.domain.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Account responses.
 *
 * Sent to client when retrieving account information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDTO {

    private UUID id;

    private String accountNumber;

    private AccountType accountType;

    private Currency currency;

    private BigDecimal balance;

    private BigDecimal availableBalance;

    private BigDecimal reservedBalance;

    private Boolean isFrozen;

    private UUID customerId;  // Reference to customer, not full object

    private LocalDateTime createdAt;

    private LocalDateTime lastTransactionAt;

    // Note: We don't expose dailyTransactionCount/Limit
    // These are internal business rules
}