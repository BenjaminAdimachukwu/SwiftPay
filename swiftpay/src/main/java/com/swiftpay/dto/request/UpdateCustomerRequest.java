package com.swiftpay.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing customer.
 *
 * All fields are optional - only provided fields are updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerRequest {

    @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
    private String firstName;

    @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
    private String lastName;

    private String phoneNumber;

    // Email and password updates handled separately for security
    // Use dedicated endpoints like:
    // PUT /customers/{id}/email
    // PUT /customers/{id}/password
}