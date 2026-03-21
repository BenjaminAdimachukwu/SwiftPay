package com.swiftpay.dto.response;

import com.swiftpay.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for Customer responses.
 *
 * Used when sending customer data TO the client.
 * Does NOT expose sensitive fields like password hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {

    private UUID id;

    private String email;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private UserRole role;

    private Integer kycLevel;

    private Boolean isEmailVerified;

    private Boolean isPhoneVerified;

    private Boolean isLocked;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Computed field - not in entity!
    // Service layer can calculate this
    public String getFullName() {
        return firstName + " " + lastName;
    }
}