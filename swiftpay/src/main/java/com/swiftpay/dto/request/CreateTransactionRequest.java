package com.swiftpay.dto.request;

import com.swiftpay.domain.enums.Currency;
import com.swiftpay.domain.enums.PaymentMethod;
import com.swiftpay.domain.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a new transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100)
    private String idempotencyKey;  // Client generates this!

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @NotNull(message = "Source account is required")
    private UUID sourceAccountId;

    @NotNull(message = "Destination account is required")
    private UUID destinationAccountId;

    @Size(max = 500)
    private String description;

    // Optional metadata (JSON string)
    private String metadata;
}