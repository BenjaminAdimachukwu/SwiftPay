package com.swiftpay.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Merchant entity representing businesses that accept payments.
 */
@Entity
@Table(
        name = "merchants",
        indexes = {
                @Index(name = "idx_merchant_code", columnList = "merchant_code", unique = true),
                @Index(name = "idx_merchant_api_key", columnList = "api_key_hash", unique = true),
                @Index(name = "idx_merchant_status", columnList = "is_active, is_verified")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant extends BaseEntity {

    @NotBlank(message = "Merchant code is required")
    @Size(min = 6, max = 20)
    @Column(name = "merchant_code", nullable = false, unique = true, length = 20)
    private String merchantCode;

    @NotBlank(message = "Business name is required")
    @Size(max = 200)
    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Size(max = 255)
    @Column(name = "address", length = 255)
    private String address;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 2)
    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "api_key_hash", nullable = false, unique = true, length = 255)
    private String apiKeyHash;

    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;

    @Column(name = "commission_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionRate = BigDecimal.valueOf(2.5);

    @Column(name = "daily_transaction_limit", precision = 19, scale = 4)
    private BigDecimal dailyTransactionLimit;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_suspended", nullable = false)
    @Builder.Default
    private Boolean isSuspended = false;

    @Size(max = 50)
    @Column(name = "business_registration_number", unique = true, length = 50)
    private String businessRegistrationNumber;

    @Size(max = 50)
    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "last_settlement_at")
    private LocalDateTime lastSettlementAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", foreignKey = @ForeignKey(name = "fk_merchant_account"))
    private Account account;

    public boolean canAcceptPayments() {
        return isActive && isVerified && !isSuspended && account != null && account.canPerformTransactions();
    }

    @Override
    public String toString() {
        return "Merchant{" +
                "id=" + getId() +
                ", merchantCode='" + merchantCode + '\'' +
                ", businessName='" + businessName + '\'' +
                '}';
    }
}
