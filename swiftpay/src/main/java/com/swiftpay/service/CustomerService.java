package com.swiftpay.service;

import com.swiftpay.domain.entity.Customer;
import com.swiftpay.domain.enums.UserRole;
import com.swiftpay.dto.request.CreateCustomerRequest;
import com.swiftpay.dto.request.UpdateCustomerRequest;
import com.swiftpay.dto.response.CustomerDTO;
import com.swiftpay.exception.CustomerLockedException;
import com.swiftpay.exception.CustomerNotFoundException;
import com.swiftpay.exception.DuplicateEmailException;
import com.swiftpay.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for Customer business logic.
 *
 * Handles:
 * - Customer creation and updates
 * - Email/phone validation
 * - KYC verification
 * - Account locking/unlocking
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    // Note: In real app, you'd also inject PasswordEncoder here

    /**
     * Create a new customer.
     *
     * Validations:
     * - Email must not already exist
     * - Phone must not already exist
     *
     * @param request Customer creation data
     * @return Created customer DTO
     * @throws DuplicateEmailException if email already exists
     */
    public CustomerDTO createCustomer(CreateCustomerRequest request) {
        log.info("Creating customer with email: {}", request.getEmail());

        // Validate: Email must be unique
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException(request.getEmail());
        }

        // Validate: Phone must be unique
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists: " + request.getPhoneNumber());
        }

        // Map request → entity
        Customer customer = Customer.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole() != null ? request.getRole() : UserRole.CUSTOMER)
                .kycLevel(0)  // Start at level 0 (not verified)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .isLocked(false)
                .failedLoginAttempts(0)
                .build();

        // In real app: Hash password before saving
        // customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Save to database
        Customer saved = customerRepository.save(customer);

        log.info("Customer created successfully: {}", saved.getId());

        // Map entity → DTO
        return mapToDTO(saved);
    }

    /**
     * Find customer by ID.
     *
     * @param id Customer UUID
     * @return Customer DTO
     * @throws CustomerNotFoundException if customer not found
     */
    @Transactional(readOnly = true)  // Read-only optimization
    public CustomerDTO findById(UUID id) {
        log.debug("Finding customer by ID: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        return mapToDTO(customer);
    }

    /**
     * Find customer by email.
     *
     * @param email Customer email
     * @return Customer DTO
     * @throws CustomerNotFoundException if customer not found
     */
    @Transactional(readOnly = true)
    public CustomerDTO findByEmail(String email) {
        log.debug("Finding customer by email: {}", email);

        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException(email));

        return mapToDTO(customer);
    }

    /**
     * Update customer information.
     *
     * Only updates provided fields (partial update).
     *
     * @param id Customer UUID
     * @param request Update data
     * @return Updated customer DTO
     * @throws CustomerNotFoundException if customer not found
     */
    public CustomerDTO updateCustomer(UUID id, UpdateCustomerRequest request) {
        log.info("Updating customer: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        // Update only provided fields
        if (request.getFirstName() != null) {
            customer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            customer.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            customer.setPhoneNumber(request.getPhoneNumber());
        }

        Customer updated = customerRepository.save(customer);

        log.info("Customer updated successfully: {}", id);

        return mapToDTO(updated);
    }

    /**
     * Verify customer's email.
     *
     * In real app, this would be called after user clicks verification link.
     *
     * @param id Customer UUID
     * @return Updated customer DTO
     */
    public CustomerDTO verifyEmail(UUID id) {
        log.info("Verifying email for customer: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.setIsEmailVerified(true);

        Customer updated = customerRepository.save(customer);

        return mapToDTO(updated);
    }

    /**
     * Verify customer's phone number.
     *
     * In real app, this would be called after SMS verification code is validated.
     *
     * @param id Customer UUID
     * @return Updated customer DTO
     */
    public CustomerDTO verifyPhone(UUID id) {
        log.info("Verifying phone for customer: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.setIsPhoneVerified(true);

        Customer updated = customerRepository.save(customer);

        return mapToDTO(updated);
    }

    /**
     * Update customer's KYC level.
     *
     * KYC levels:
     * 0 - Not verified (default)
     * 1 - Basic verification (email + phone)
     * 2 - Identity verified (ID document)
     * 3 - Full verification (ID + address + income proof)
     *
     * @param id Customer UUID
     * @param kycLevel New KYC level (0-3)
     * @return Updated customer DTO
     */
    public CustomerDTO updateKycLevel(UUID id, Integer kycLevel) {
        log.info("Updating KYC level for customer {} to level {}", id, kycLevel);

        // Validate KYC level
        if (kycLevel < 0 || kycLevel > 3) {
            throw new IllegalArgumentException("KYC level must be between 0 and 3");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.setKycLevel(kycLevel);

        Customer updated = customerRepository.save(customer);

        log.info("KYC level updated successfully");

        return mapToDTO(updated);
    }

    /**
     * Lock customer account.
     *
     * Used for security reasons:
     * - Too many failed login attempts
     * - Suspicious activity detected
     * - Fraud investigation
     *
     * @param id Customer UUID
     * @return Updated customer DTO
     */
    public CustomerDTO lockCustomer(UUID id) {
        log.warn("Locking customer account: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.setIsLocked(true);

        Customer updated = customerRepository.save(customer);

        log.warn("Customer account locked: {}", id);

        return mapToDTO(updated);
    }

    /**
     * Unlock customer account.
     *
     * Used after security review or customer verification.
     *
     * @param id Customer UUID
     * @return Updated customer DTO
     */
    public CustomerDTO unlockCustomer(UUID id) {
        log.info("Unlocking customer account: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.setIsLocked(false);
        customer.setFailedLoginAttempts(0);  // Reset failed attempts

        Customer updated = customerRepository.save(customer);

        log.info("Customer account unlocked: {}", id);

        return mapToDTO(updated);
    }

    /**
     * Get all active customers (paginated).
     *
     * Active = not deleted and not locked.
     *
     * @param pageable Pagination parameters
     * @return Page of customer DTOs
     */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> findActiveCustomers(Pageable pageable) {
        log.debug("Finding active customers, page: {}", pageable.getPageNumber());

        Page<Customer> customers = customerRepository.findActiveCustomers(pageable);

        return customers.map(this::mapToDTO);
    }

    /**
     * Search customers by name.
     *
     * @param searchTerm Search term (first or last name)
     * @param pageable Pagination parameters
     * @return Page of matching customer DTOs
     */
    @Transactional(readOnly = true)
    public Page<CustomerDTO> searchByName(String searchTerm, Pageable pageable) {
        log.debug("Searching customers by name: {}", searchTerm);

        Page<Customer> customers = customerRepository.searchByName(searchTerm, pageable);

        return customers.map(this::mapToDTO);
    }

    /**
     * Soft delete customer.
     *
     * Sets isDeleted flag instead of actually deleting from database.
     *
     * @param id Customer UUID
     */
    public void deleteCustomer(UUID id) {
        log.warn("Soft deleting customer: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.softDelete();  // Method from BaseEntity

        customerRepository.save(customer);

        log.warn("Customer soft deleted: {}", id);
    }

    // -------------------------
    // Helper Methods
    // -------------------------

    /**
     * Map Customer entity to CustomerDTO.
     *
     * Important: Never expose entity directly!
     * Always convert to DTO before returning.
     */
    private CustomerDTO mapToDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .email(customer.getEmail())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .phoneNumber(customer.getPhoneNumber())
                .role(customer.getRole())
                .kycLevel(customer.getKycLevel())
                .isEmailVerified(customer.getIsEmailVerified())
                .isPhoneVerified(customer.getIsPhoneVerified())
                .isLocked(customer.getIsLocked())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}