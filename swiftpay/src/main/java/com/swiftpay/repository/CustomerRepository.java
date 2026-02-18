package com.swiftpay.repository;

import com.swiftpay.domain.entity.Customer;
import com.swiftpay.domain.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository  extends JpaRepository<Customer, UUID>,JpaSpecificationExecutor<Customer> {

    /**
     * Find customer by email address.
     * Email is unique, so returns Optional.
     *
     * @param email Customer's email address
     * @return Optional containing customer if found
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Find customer by email (case-insensitive).
     * Useful for login scenarios where case shouldn't matter.
     *
     * @param email Email address (any case)
     * @return Optional containing customer if found
     */
    Optional<Customer> findByEmailIgnoreCase(String email);

    /**
     * Find customer by phone number.
     *
     * @param phoneNumber Customer's phone number
     * @return Optional containing customer if found
     */
    Optional<Customer> findByPhoneNumber(String phoneNumber);

    /**
     * Check if email already exists.
     * Useful for validation before creating new customer.
     *
     * @param email Email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone number already exists.
     *
     * @param phoneNumber Phone number to check
     * @return true if phone exists, false otherwise
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Find all customers with a specific role.
     *
     * @param role User role to filter by
     * @param pageable Pagination parameters
     * @return Page of customers with the specified role
     */
    Page<Customer> findByRole(UserRole role, Pageable pageable);

    /**
     * Find customers by KYC verification level.
     * Useful for compliance reporting.
     *
     * @param kycLevel KYC level (0-3)
     * @param pageable Pagination parameters
     * @return Page of customers with specified KYC level
     */
    Page<Customer> findByKycLevel(Integer kycLevel, Pageable pageable);

    /**
     * Find customers with email verified.
     *
     * @param isEmailVerified Email verification status
     * @param pageable Pagination parameters
     * @return Page of customers
     */
    Page<Customer> findByIsEmailVerified(Boolean isEmailVerified, Pageable pageable);

    /**
     * Find customers with phone verified.
     *
     * @param isPhoneVerified Phone verification status
     * @param pageable Pagination parameters
     * @return Page of customers
     */
    Page<Customer> findByIsPhoneVerified(Boolean isPhoneVerified, Pageable pageable);

    /**
     * Find locked customer accounts.
     * Useful for security monitoring.
     *
     * @param isLocked Account lock status
     * @param pageable Pagination parameters
     * @return Page of locked customers
     */
    Page<Customer> findByIsLocked(Boolean isLocked, Pageable pageable);

    /**
     * Find customers created within a date range.
     * Useful for growth analytics.
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters
     * @return Page of customers created in date range
     */
    Page<Customer> findByCreatedAtBetween(LocalDateTime startDate,
                                          LocalDateTime endDate,
                                          Pageable pageable);

    /**
     * Find active customers (not deleted, not locked).
     * Custom query using JPQL.
     *
     * @param pageable Pagination parameters
     * @return Page of active customers
     */
    @Query("SELECT c FROM Customer c WHERE c.isDeleted = false AND c.isLocked = false")
    Page<Customer> findActiveCustomers(Pageable pageable);

    /**
     * Find customers with failed login attempts above threshold.
     * Useful for security monitoring.
     *
     * @param threshold Minimum failed login attempts
     * @return List of customers with excessive failed logins
     */
    @Query("SELECT c FROM Customer c WHERE c.failedLoginAttempts >= :threshold AND c.isLocked = false")
    List<Customer> findCustomersWithExcessiveFailedLogins(@Param("threshold") Integer threshold);

    /**
     * Count customers by KYC level.
     * Useful for compliance reporting.
     *
     * @param kycLevel KYC level to count
     * @return Number of customers with specified KYC level
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.kycLevel = :kycLevel AND c.isDeleted = false")
    Long countByKycLevel(@Param("kycLevel") Integer kycLevel);

    /**
     * Find customers who haven't verified email within specified days.
     * Useful for reminder campaigns.
     *
     * @param cutoffDate Number of days since creation
     * @return List of customers needing email verification reminder
     */
    @Query("SELECT c FROM Customer c WHERE c.isEmailVerified = false " +
            "AND c.createdAt < :cutoffDate AND c.isDeleted = false")
    List<Customer> findUnverifiedEmailCustomers(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Search customers by name (first or last name contains search term).
     * Case-insensitive search.
     *
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching customers
     */
    @Query("SELECT c FROM Customer c WHERE c.isDeleted = false AND " +
            "(LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Customer> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find customers by multiple criteria (example of complex query).
     *
     * @param role User role
     * @param kycLevel KYC level
     * @param isEmailVerified Email verification status
     * @param pageable Pagination parameters
     * @return Page of matching customers
     */
    @Query("SELECT c FROM Customer c WHERE c.role = :role " +
            "AND c.kycLevel = :kycLevel " +
            "AND c.isEmailVerified = :isEmailVerified " +
            "AND c.isDeleted = false")
    Page<Customer> findByRoleAndKycLevelAndEmailVerified(
            @Param("role") UserRole role,
            @Param("kycLevel") Integer kycLevel,
            @Param("isEmailVerified") Boolean isEmailVerified,
            Pageable pageable
    );

    /**
     * Get customer statistics.
     * Returns: total customers, verified emails, verified phones, locked accounts.
     *
     * @return Object array with statistics
     */
    @Query("SELECT " +
            "COUNT(c), " +
            "SUM(CASE WHEN c.isEmailVerified = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN c.isPhoneVerified = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN c.isLocked = true THEN 1 ELSE 0 END) " +
            "FROM Customer c WHERE c.isDeleted = false")
    Object[] getCustomerStatistics();

}
