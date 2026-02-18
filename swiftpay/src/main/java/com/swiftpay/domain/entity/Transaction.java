package com.swiftpay.domain.entity;

import com.swiftpay.domain.enums.Currency;
import com.swiftpay.domain.enums.PaymentMethod;
import com.swiftpay.domain.enums.TransactionStatus;
import com.swiftpay.domain.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction entity representing a financial transaction in the system.
 */
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transaction_reference", columnList = "transaction_reference", unique = true),
                @Index(name = "idx_transaction_idempotency_key", columnList = "idempotency_key", unique = true),
                @Index(name = "idx_transaction_source_account", columnList = "source_account_id"),
                @Index(name = "idx_transaction_destination_account", columnList = "destination_account_id"),
                @Index(name = "idx_transaction_status", columnList = "status"),
                @Index(name = "idx_transaction_type", columnList = "transaction_type"),
                @Index(name = "idx_transaction_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @NotBlank(message = "Transaction reference is required")
    @Size(min = 18, max = 50)
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 50)
    private String transactionReference;

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 100)
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @NotNull(message = "Transaction status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.INITIATED;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Column(name = "processing_fee", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(name = "net_amount", precision = 19, scale = 4)
    private BigDecimal netAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", foreignKey = @ForeignKey(name = "fk_transaction_source_account"))
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id", foreignKey = @ForeignKey(name = "fk_transaction_destination_account"))
    private Account destinationAccount;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Size(max = 100)
    @Column(name = "gateway_reference", length = 100)
    private String gatewayReference;

    @Size(max = 50)
    @Column(name = "gateway_response_code", length = 50)
    private String gatewayResponseCode;

    @Size(max = 500)
    @Column(name = "gateway_response_message", length = 500)
    private String gatewayResponseMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_transaction_id", foreignKey = @ForeignKey(name = "fk_transaction_parent"))
    private Transaction parentTransaction;

    @Size(max = 45)
    @Column(name = "client_ip_address", length = 45)
    private String clientIpAddress;

    @Column(name = "initiated_at", nullable = false)
    @Builder.Default
    private LocalDateTime initiatedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Size(max = 50)
    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Size(max = 500)
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "is_test_transaction", nullable = false)
    @Builder.Default
    private Boolean isTestTransaction = false;

    @PrePersist
    @PreUpdate
    protected void calculateNetAmount() {
        if (amount != null && processingFee != null) {
            netAmount = amount.subtract(processingFee);
        }
    }

    public boolean isCompleted() {
        return status.isTerminal();
    }

    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESS;
    }

    public void markAsSuccessful() {
        this.status = TransactionStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorCode, String errorMessage) {
        this.status = TransactionStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + getId() +
                ", transactionReference='" + transactionReference + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                '}';
    }
}
