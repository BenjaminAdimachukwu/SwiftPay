package com.swiftpay.dto.response;

import com.swiftpay.domain.enums.Currency;
import com.swiftpay.domain.enums.PaymentMethod;
import com.swiftpay.domain.enums.TransactionStatus;
import com.swiftpay.domain.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Transaction responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {

    private UUID id;

    private String transactionReference;

    private TransactionType transactionType;

    private TransactionStatus status;

    private PaymentMethod paymentMethod;

    private BigDecimal amount;

    private Currency currency;

    private BigDecimal processingFee;

    private BigDecimal netAmount;

    private String description;

    // Account references (just IDs)
    private UUID sourceAccountId;
    private String sourceAccountNumber;

    private UUID destinationAccountId;
    private String destinationAccountNumber;

    private LocalDateTime initiatedAt;

    private LocalDateTime completedAt;

    // Error info (if failed)
    private String errorCode;
    private String errorMessage;

    // Gateway info
    private String gatewayReference;

    // Note: We don't expose idempotency key - internal detail
    // Note: We don't expose retry count - internal detail
}