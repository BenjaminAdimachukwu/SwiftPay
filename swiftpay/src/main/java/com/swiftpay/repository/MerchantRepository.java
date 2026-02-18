package com.swiftpay.repository;

import com.swiftpay.domain.entity.Merchant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID>,JpaSpecificationExecutor<Merchant> {

    /**
     * Find merchant by business name.
     *
     * @param businessName Business name
     * @return Optional containing merchant if found
     */
    Optional<Merchant> findByBusinessName(String businessName);

    /**
     * Find merchant by business email.
     *
     * @param email Business email address
     * @return Optional containing merchant if found
     */
    Optional<Merchant> findByEmail(String email);

    /**
     * Find merchant by API key.
     * Used for authentication in API requests.
     *
     * @param apiKeyHash Merchant's API key
     * @return Optional containing merchant if found
     */
    Optional<Merchant> findByApiKeyHash(String apiKeyHash);

    /**
     * Check if API key exists.
     *
     * @param apiKeyHash API key to check
     * @return true if API key exists
     */
    boolean existsByApiKeyHash(String apiKeyHash);

    /**
     * Check if business email exists.
     *
     * @param email Business email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find merchant by business registration number.
     *
     * @param businessRegistrationNumber Registration number
     * @return Optional containing merchant if found
     */
    Optional<Merchant> findByBusinessRegistrationNumber(String businessRegistrationNumber);

    /**
     * Find merchant by tax ID.
     *
     * @param taxId Tax ID
     * @return Optional containing merchant if found
     */
    Optional<Merchant> findByTaxId(String taxId);

    /**
     * Find active merchants (verified and not suspended).
     *
     * @param pageable Pagination parameters
     * @return Page of active merchants
     */
    @Query("SELECT m FROM Merchant m WHERE m.isVerified = true " +
            "AND m.isSuspended = false AND m.isDeleted = false")
    Page<Merchant> findActiveMerchants(Pageable pageable);

    /**
     * Find verified merchants.
     *
     * @param isVerified Verification status
     * @param pageable Pagination parameters
     * @return Page of merchants
     */
    Page<Merchant> findByIsVerified(Boolean isVerified, Pageable pageable);

    /**
     * Find merchants created within date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Page of merchants
     */
    Page<Merchant> findByCreatedAtBetween(LocalDateTime startDate,
                                          LocalDateTime endDate,
                                          Pageable pageable);


    /**
     * Find merchants with commission rate above threshold.
     *
     * @param threshold Minimum commission rate
     * @param pageable Pagination parameters
     * @return Page of merchants
     */
    @Query("SELECT m FROM Merchant m WHERE m.commissionRate >= :threshold " +
            "AND m.isDeleted = false")
    Page<Merchant> findByCommissionRateGreaterThanEqual(
            @Param("threshold") BigDecimal threshold,
            Pageable pageable
    );

    /**
     * Find merchants with webhook configured.
     *
     * @param pageable Pagination parameters
     * @return Page of merchants with webhooks
     */
    @Query("SELECT m FROM Merchant m WHERE m.webhookUrl IS NOT NULL " +
            "AND m.isDeleted = false")
    Page<Merchant> findMerchantsWithWebhook(Pageable pageable);

    /**
     * Find merchants pending verification.
     *
     * @return List of unverified merchants
     */
    @Query("SELECT m FROM Merchant m WHERE m.isVerified = false " +
            "AND m.isSuspended = false AND m.isDeleted = false")
    List<Merchant> findMerchantsPendingVerification();

    /**
     * Search merchants by business name (case-insensitive, partial match).
     *
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of matching merchants
     */
    @Query("SELECT m FROM Merchant m WHERE m.isDeleted = false AND " +
            "LOWER(m.businessName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Merchant> searchByBusinessName(@Param("searchTerm") String searchTerm,
                                        Pageable pageable);


    /**
     * Count merchants by verification status.
     *
     * @param isVerified Verification status
     * @return Number of merchants
     */
    @Query("SELECT COUNT(m) FROM Merchant m WHERE m.isVerified = :isVerified " +
            "AND m.isDeleted = false")
    Long countByVerificationStatus(@Param("isVerified") Boolean isVerified);

    /**
     * Get merchant statistics.
     * Returns: total merchants, verified count, suspended count, avg commission rate.
     *
     * @return Object array with statistics
     */
    @Query("SELECT " +
            "COUNT(m), " +
            "SUM(CASE WHEN m.isVerified = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN m.isSuspended = true THEN 1 ELSE 0 END), " +
            "AVG(m.commissionRate) " +
            "FROM Merchant m WHERE m.isDeleted = false")
    Object[] getMerchantStatistics();

    /**
     * Find merchants with last settlement before date.
     * Useful for identifying merchants needing settlement.
     *
     * @param cutoffDate Date threshold
     * @return List of merchants needing settlement
     */
    @Query("SELECT m FROM Merchant m WHERE m.lastSettlementAt < :cutoffDate " +
            "AND m.isVerified = true AND m.isSuspended = false AND m.isDeleted = false")
    List<Merchant> findMerchantsNeedingSettlement(@Param("cutoffDate") LocalDateTime cutoffDate);
}
