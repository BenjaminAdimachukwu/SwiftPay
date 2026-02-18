package com.swiftpay.domain.entity;

import com.swiftpay.domain.enums.AccountType;
import com.swiftpay.domain.enums.Currency;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


/**
 * Account entity representing a financial account in the system.
 */
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_account_number", columnList = "account_number", unique = true),
                @Index(name = "idx_account_customer", columnList = "customer_id"),
                @Index(name = "idx_account_status", columnList = "is_active, is_frozen"),
                @Index(name = "idx_account_type_currency", columnList = "account_type, currency")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_type_currency",
                        columnNames = {"customer_id", "account_type", "currency"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account extends BaseEntity {
    @NotBlank(message = "Account number is required")
    @Size(min = 10, max = 34)
    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @NotBlank(message = "Account name is required")
    @Size(max = 100)
    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @NotNull(message = "Account type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 30)
    private AccountType accountType;

    @NotNull(message = "Currency is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "reserved_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal reservedBalance = BigDecimal.ZERO;

    @Column(name = "overdraft_limit", precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal overdraftLimit = BigDecimal.ZERO;

    @Column(name = "daily_limit", precision = 19, scale = 4)
    private BigDecimal dailyLimit;

    @Column(name = "monthly_limit", precision = 19, scale = 4)
    private BigDecimal monthlyLimit;

    @Column(name = "single_transaction_limit", precision = 19, scale = 4)
    private BigDecimal singleTransactionLimit;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_frozen", nullable = false)
    @Builder.Default
    private Boolean isFrozen = false;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Size(max = 255)
    @Column(name = "freeze_reason", length = 255)
    private String freezeReason;

    @Column(name = "daily_transaction_count", nullable = false)
    @Builder.Default
    private Integer dailyTransactionCount = 0;

    @Column(name = "daily_transaction_limit", nullable = false)
    @Builder.Default
    private Integer dailyTransactionLimit = 10;  // Default limit

    @Column(name = "last_transaction_date")
    private LocalDateTime lastTransactionDate;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;


    @NotNull(message = "Customer is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_account_customer"))
    private Customer customer;

    @OneToMany(mappedBy = "sourceAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Transaction> outgoingTransactions = new HashSet<>();

    @OneToMany(mappedBy = "destinationAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Transaction> incomingTransactions = new HashSet<>();

    public boolean canPerformTransactions() {
        return isActive && !isFrozen && customer != null && customer.canPerformTransactions();
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return availableBalance.add(overdraftLimit).compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException("Insufficient balance for debit");
        }
        balance = balance.subtract(amount);
        availableBalance = availableBalance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        balance = balance.add(amount);
        availableBalance = availableBalance.add(amount);
    }

    public void freeze(String reason) {
        this.isFrozen = true;
        this.freezeReason = reason;
    }

    public void unfreeze() {
        this.isFrozen = false;
        this.freezeReason = null;
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + getId() +
                ", accountNumber='" + accountNumber + '\'' +
                ", accountType=" + accountType +
                ", balance=" + balance +
                '}';
    }
}
