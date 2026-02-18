package com.swiftpay.domain.entity;

import com.swiftpay.domain.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Customer entity representing users of the payment system.
 */
@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customer_email", columnList = "email", unique = true),
                @Index(name = "idx_customer_phone", columnList = "phone_number"),
                @Index(name = "idx_customer_status", columnList = "is_active, is_email_verified"),
                @Index(name = "idx_customer_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer  extends BaseEntity {
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50)
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50)
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Size(max = 255)
    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Size(max = 255)
    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Size(max = 100)
    @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100)
    @Column(name = "state", length = 100)
    private String state;

    @Size(max = 20)
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Size(min = 2, max = 2)
    @Column(name = "country", length = 2)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private UserRole role = UserRole.CUSTOMER;

    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "is_phone_verified", nullable = false)
    @Builder.Default
    private Boolean isPhoneVerified = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "kyc_verified", nullable = false)
    @Builder.Default
    private Boolean kycVerified = false;

    @Column(name = "kyc_verified_at")
    private LocalDate kycVerifiedAt;

    @Column(name = "kyc_level")
    @Builder.Default
    private Integer kycLevel = 0;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private LocalDate lastLoginAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Account> accounts = new HashSet<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean canPerformTransactions() {
        return isActive && !isLocked && isEmailVerified && kycVerified;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.isLocked = true;
        }
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + getId() +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
