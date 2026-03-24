package com.swiftpay.controller;

import com.swiftpay.dto.request.CreateCustomerRequest;
import com.swiftpay.dto.request.UpdateCustomerRequest;
import com.swiftpay.dto.response.CustomerDTO;
import com.swiftpay.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Customer operations.
 *
 * Base URL: /api/customers
 *
 * Provides endpoints for:
 * - Customer creation and updates
 * - Email/phone verification
 * - KYC management
 * - Account locking/unlocking
 * - Customer search
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    // ========================================
    // CREATE Operations
    // ========================================

    /**
     * Create a new customer.
     *
     * POST /api/customers
     *
     * Request body:
     * {
     *   "email": "john@example.com",
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "phoneNumber": "+2348012345678",
     *   "role": "CUSTOMER"
     * }
     *
     * Response: HTTP 201 CREATED with CustomerDTO
     *
     * @param request Customer creation data
     * @return Created customer DTO
     */
    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        log.info("Creating customer with email: {}", request.getEmail());

        CustomerDTO customer = customerService.createCustomer(request);

        // Return HTTP 201 CREATED
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }

    // ========================================
    // READ Operations
    // ========================================

    /**
     * Get customer by ID.
     *
     * GET /api/customers/{id}
     *
     * Response: HTTP 200 OK with CustomerDTO
     * Error: HTTP 404 if not found
     *
     * @param id Customer UUID
     * @return Customer DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable UUID id) {

        log.debug("Getting customer by ID: {}", id);

        CustomerDTO customer = customerService.findById(id);

        return ResponseEntity.ok(customer);
    }

    /**
     * Get customer by email.
     *
     * GET /api/customers/email/{email}
     *
     * Example: GET /api/customers/email/john@example.com
     *
     * Response: HTTP 200 OK with CustomerDTO
     * Error: HTTP 404 if not found
     *
     * @param email Customer email
     * @return Customer DTO
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<CustomerDTO> getCustomerByEmail(@PathVariable String email) {

        log.debug("Getting customer by email: {}", email);

        CustomerDTO customer = customerService.findByEmail(email);

        return ResponseEntity.ok(customer);
    }

    /**
     * Search customers by name.
     *
     * GET /api/customers/search?q=John&page=0&size=20
     *
     * Query parameters:
     * - q: Search term (required)
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     *
     * Response: HTTP 200 OK with Page<CustomerDTO>
     *
     * @param searchTerm Search term (first or last name)
     * @param page Page number
     * @param size Page size
     * @return Page of matching customers
     */
    @GetMapping("/search")
    public ResponseEntity<Page<CustomerDTO>> searchCustomers(
            @RequestParam(name = "q") String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Searching customers with term: {}", searchTerm);

        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerDTO> customers = customerService.searchByName(searchTerm, pageable);

        return ResponseEntity.ok(customers);
    }

    /**
     * Get active customers (not deleted, not locked).
     *
     * GET /api/customers/active?page=0&size=20
     *
     * Query parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     *
     * Response: HTTP 200 OK with Page<CustomerDTO>
     *
     * @param page Page number
     * @param size Page size
     * @return Page of active customers
     */
    @GetMapping("/active")
    public ResponseEntity<Page<CustomerDTO>> getActiveCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting active customers, page: {}", page);

        Pageable pageable = PageRequest.of(page, size);
        Page<CustomerDTO> customers = customerService.findActiveCustomers(pageable);

        return ResponseEntity.ok(customers);
    }

    // ========================================
    // UPDATE Operations
    // ========================================

    /**
     * Update customer information.
     *
     * PUT /api/customers/{id}
     *
     * Request body (all fields optional):
     * {
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "phoneNumber": "+2348012345678"
     * }
     *
     * Response: HTTP 200 OK with updated CustomerDTO
     * Error: HTTP 404 if customer not found
     *
     * @param id Customer UUID
     * @param request Update data
     * @return Updated customer DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        log.info("Updating customer: {}", id);

        CustomerDTO customer = customerService.updateCustomer(id, request);

        return ResponseEntity.ok(customer);
    }

    /**
     * Verify customer's email.
     *
     * POST /api/customers/{id}/verify-email
     *
     * In real app, this would be called after user clicks verification link.
     *
     * Response: HTTP 200 OK with updated CustomerDTO
     *
     * @param id Customer UUID
     * @return Updated customer DTO
     */
    @PostMapping("/{id}/verify-email")
    public ResponseEntity<CustomerDTO> verifyEmail(@PathVariable UUID id) {

        log.info("Verifying email for customer: {}", id);

        CustomerDTO customer = customerService.verifyEmail(id);

        return ResponseEntity.ok(customer);
    }

    /**
     * Verify customer's phone number.
     *
     * POST /api/customers/{id}/verify-phone
     *
     * In real app, this would be called after SMS code verification.
     *
     * Response: HTTP 200 OK with updated CustomerDTO
     *
     * @param id Customer UUID
     * @return Updated customer DTO
     */
    @PostMapping("/{id}/verify-phone")
    public ResponseEntity<CustomerDTO> verifyPhone(@PathVariable UUID id) {

        log.info("Verifying phone for customer: {}", id);

        CustomerDTO customer = customerService.verifyPhone(id);

        return ResponseEntity.ok(customer);
    }

    /**
     * Update customer's KYC level.
     *
     * PUT /api/customers/{id}/kyc
     *
     * Request body:
     * {
     *   "kycLevel": 2
     * }
     *
     * KYC levels:
     * 0 - Not verified
     * 1 - Basic (email + phone)
     * 2 - Identity verified
     * 3 - Full verification
     *
     * Response: HTTP 200 OK with updated CustomerDTO
     *
     * @param id Customer UUID
     * @param kycLevel New KYC level (0-3)
     * @return Updated customer DTO
     */
    @PutMapping("/{id}/kyc")
    public ResponseEntity<CustomerDTO> updateKycLevel(
            @PathVariable UUID id,
            @RequestParam Integer kycLevel) {

        log.info("Updating KYC level for customer {} to {}", id, kycLevel);

        CustomerDTO customer = customerService.updateKycLevel(id, kycLevel);

        return ResponseEntity.ok(customer);
    }

    /**
     * Lock customer account.
     *
     * POST /api/customers/{id}/lock
     *
     * Used for security reasons (fraud, suspicious activity).
     *
     * Response: HTTP 200 OK with updated CustomerDTO
     *
     * @param id Customer UUID
     * @return Updated customer DTO
     */
    @PostMapping("/{id}/lock")
    public ResponseEntity<CustomerDTO> lockCustomer(@PathVariable UUID id) {

        log.warn("Locking customer: {}", id);

        CustomerDTO customer = customerService.lockCustomer(id);

        return ResponseEntity.ok(customer);
    }

    /**
     * Unlock customer account.
     *
     * POST /api/customers/{id}/unlock
     *
     * Used after security review or verification.
     *
     * Response: HTTP 200 OK with updated CustomerDTO
     *
     * @param id Customer UUID
     * @return Updated customer DTO
     */
    @PostMapping("/{id}/unlock")
    public ResponseEntity<CustomerDTO> unlockCustomer(@PathVariable UUID id) {

        log.info("Unlocking customer: {}", id);

        CustomerDTO customer = customerService.unlockCustomer(id);

        return ResponseEntity.ok(customer);
    }

    // ========================================
    // DELETE Operations
    // ========================================

    /**
     * Delete customer (soft delete).
     *
     * DELETE /api/customers/{id}
     *
     * Sets isDeleted flag instead of actually deleting from database.
     *
     * Response: HTTP 204 NO CONTENT
     * Error: HTTP 404 if customer not found
     *
     * @param id Customer UUID
     * @return No content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {

        log.warn("Deleting customer: {}", id);

        customerService.deleteCustomer(id);

        // HTTP 204 NO CONTENT (successful deletion, no response body)
        return ResponseEntity.noContent().build();
    }
}