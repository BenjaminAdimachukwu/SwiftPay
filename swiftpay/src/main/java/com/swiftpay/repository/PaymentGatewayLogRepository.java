package com.swiftpay.repository;

import com.swiftpay.domain.entity.PaymentGatewayLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for PaymentGatewayLog entity.
 *
 * Provides queries for payment gateway integration monitoring, debugging, and reconciliation.
 */
@Repository
public interface PaymentGatewayLogRepository extends JpaRepository<PaymentGatewayLog, UUID>, JpaSpecificationExecutor<PaymentGatewayLog> {

    /**
     * Find gateway logs by transaction ID.
     * All gateway interactions for a specific transaction.
     *
     * @param transactionId Transaction UUID
     * @return List of gateway logs
     */
    List<PaymentGatewayLog> findByTransactionId(UUID transactionId);


    /**
     * Find gateway logs by gateway name.
     * Example: All Stripe, PayPal, or Flutterwave logs.
     *
     * @param gatewayName Gateway name
     * @param pageable Pagination parameters
     * @return Page of gateway logs
     */
    Page<PaymentGatewayLog> findByGatewayName(String gatewayName, Pageable pageable);

    /**
     * Find gateway logs by HTTP method.
     * Example: All POST, GET, PUT requests.
     *
     * @param method HTTP method
     * @param pageable Pagination parameters
     * @return Page of gateway logs
     */
    Page<PaymentGatewayLog> findByRequestMethod(String method, Pageable pageable);

    /**
     * Find gateway logs by HTTP status code.
     * Example: All 200, 400, 500 responses.
     *
     * @param statusCode HTTP status code
     * @param pageable Pagination parameters
     * @return Page of gateway logs
     */
    Page<PaymentGatewayLog> findByResponseStatusCode(Integer statusCode, Pageable pageable);

    /**
     * Find failed gateway requests (non-2xx status codes).
     * Useful for error monitoring.
     *
     * @param pageable Pagination parameters
     * @return Page of failed requests
     */
    @Query("SELECT g FROM PaymentGatewayLog g WHERE g.responseStatusCode < 200 OR g.responseStatusCode >= 300")
    Page<PaymentGatewayLog> findFailedRequests(Pageable pageable);

    /**
     * Find successful gateway requests (2xx status codes).
     *
     * @param pageable Pagination parameters
     * @return Page of successful requests
     */
    @Query("SELECT g FROM PaymentGatewayLog g WHERE g.responseStatusCode >= 200 AND g.responseStatusCode < 300")
    Page<PaymentGatewayLog> findSuccessfulRequests(Pageable pageable);

    /**
     * Find gateway logs within date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Page of gateway logs
     */
    Page<PaymentGatewayLog> findByCreatedAtBetween(LocalDateTime startDate,
                                                   LocalDateTime endDate,
                                                   Pageable pageable);


    /**
     * Find slow gateway requests (response time above threshold).
     * Performance monitoring.
     *
     * @param thresholdMs Threshold in milliseconds
     * @param pageable Pagination parameters
     * @return Page of slow requests
     */
    @Query("SELECT g FROM PaymentGatewayLog g WHERE g.responseTimeMs > :threshold")
    Page<PaymentGatewayLog> findSlowRequests(
            @Param("threshold") Long thresholdMs,
            Pageable pageable
    );

    /**
     * Find gateway logs by endpoint.
     * Example: All requests to /api/payments/charge.
     *
     * @param endpoint API endpoint
     * @param pageable Pagination parameters
     * @return Page of gateway logs
     */
    @Query("SELECT g FROM PaymentGatewayLog g WHERE g.requestUrl LIKE CONCAT('%', :endpoint, '%')")
    Page<PaymentGatewayLog> findByEndpointContaining(
            @Param("endpoint") String endpoint,
            Pageable pageable
    );

    /**
     * Find gateway logs for specific gateway and status code.
     * Example: All Stripe 500 errors.
     *
     * @param gatewayName Gateway name
     * @param statusCode HTTP status code
     * @param pageable Pagination parameters
     * @return Page of gateway logs
     */
    Page<PaymentGatewayLog> findByGatewayNameAndResponseStatusCode(
            String gatewayName,
            Integer statusCode,
            Pageable pageable
    );


    /**
     * Calculate average response time for a gateway.
     * Performance metrics.
     *
     * @param gatewayName Gateway name
     * @param startDate Start date
     * @param endDate End date
     * @return Average response time in milliseconds
     */
    @Query("SELECT AVG(g.responseTimeMs) FROM PaymentGatewayLog g " +
            "WHERE g.gatewayName = :gatewayName " +
            "AND g.createdAt BETWEEN :startDate AND :endDate")
    Double calculateAverageResponseTime(
            @Param("gatewayName") String gatewayName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get gateway statistics for date range.
     * Returns: total requests, successful, failed, avg response time.
     *
     * @param gatewayName Gateway name
     * @param startDate Start date
     * @param endDate End date
     * @return Object array with statistics
     */
    @Query("SELECT " +
            "COUNT(g), " +
            "SUM(CASE WHEN g.responseStatusCode >= 200 AND g.responseStatusCode < 300 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN g.responseStatusCode < 200 OR g.responseStatusCode >= 300 THEN 1 ELSE 0 END), " +
            "AVG(g.responseTimeMs) " +
            "FROM PaymentGatewayLog g " +
            "WHERE g.gatewayName = :gatewayName " +
            "AND g.createdAt BETWEEN :startDate AND :endDate")
    Object[] getGatewayStatistics(
            @Param("gatewayName") String gatewayName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find recent logs for a transaction.
     * Latest gateway interactions for debugging.
     *
     * @param transactionId Transaction UUID
     * @param pageable Pagination parameters
     * @return List of recent gateway logs
     */
    @Query("SELECT g FROM PaymentGatewayLog g WHERE g.transaction.id = :transactionId " +
            "ORDER BY g.createdAt DESC")
    List<PaymentGatewayLog> findRecentLogsByTransaction(
            @Param("transactionId") UUID transactionId,
            Pageable pageable
    );

    /**
     * Find timeout errors (response time above threshold).
     *
     * @param timeoutThreshold Timeout threshold in milliseconds
     * @param pageable Pagination parameters
     * @return Page of timeout logs
     */
    @Query("SELECT g FROM PaymentGatewayLog g WHERE g.responseTimeMs > :timeout " +
            "OR g.responseStatusCode = 408 OR g.responseStatusCode = 504")
    Page<PaymentGatewayLog> findTimeoutErrors(
            @Param("timeout") Long timeoutThreshold,
            Pageable pageable
    );

    /**
     * Count requests by gateway and date range.
     *
     * @param gatewayName Gateway name
     * @param startDate Start date
     * @param endDate End date
     * @return Number of requests
     */
    @Query("SELECT COUNT(g) FROM PaymentGatewayLog g " +
            "WHERE g.gatewayName = :gatewayName " +
            "AND g.createdAt BETWEEN :startDate AND :endDate")
    Long countByGatewayAndDateRange(
            @Param("gatewayName") String gatewayName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find gateway logs with error messages.
     * Logs where errorMessage is not null.
     *
     * @param pageable Pagination parameters
     * @return Page of error logs
     */
    @Query("SELECT g FROM PaymentGatewayLog g WHERE g.errorMessage IS NOT NULL")
    Page<PaymentGatewayLog> findLogsWithErrors(Pageable pageable);

    /**
     * Find duplicate requests (same transaction, same gateway, within time window).
     * Useful for identifying retry attempts.
     *
     * @param transactionId Transaction UUID
     * @param gatewayName Gateway name
     * @param timeWindow Start of time window
     * @return List of potential duplicate requests
     */
    @Query("SELECT g FROM PaymentGatewayLog g WHERE g.transaction.id = :transactionId " +
            "AND g.gatewayName = :gatewayName " +
            "AND g.createdAt >= :timeWindow " +
            "ORDER BY g.createdAt ASC")
    List<PaymentGatewayLog> findDuplicateRequests(
            @Param("transactionId") UUID transactionId,
            @Param("gatewayName") String gatewayName,
            @Param("timeWindow") LocalDateTime timeWindow
    );
}
