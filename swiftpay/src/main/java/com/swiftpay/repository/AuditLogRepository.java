package com.swiftpay.repository;

import com.swiftpay.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Find audit logs by user ID.
     * Complete activity history for a user.
     *
     * @param userId User UUID
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find audit logs by entity type.
     * Example: All logs related to "Customer" entity.
     *
     * @param entityType Entity type (Customer, Account, Transaction, etc.)
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    /**
     * Find audit logs by entity ID.
     * All changes made to a specific entity.
     *
     * @param entityId Entity UUID
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    Page<AuditLog> findByEntityId(UUID entityId, Pageable pageable);

    /**
     * Find audit logs by action type.
     * Example: All CREATE, UPDATE, or DELETE actions.
     *
     * @param action Action type
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * Find audit logs within date range.
     *
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    Page<AuditLog> findByCreatedAtBetween(LocalDateTime startDate,
                                         LocalDateTime endDate,
                                         Pageable pageable);

    /**
     * Find audit logs by IP address.
     * Useful for security investigation.
     *
     * @param ipAddress IP address
     * @param pageable Pagination parameters
     * @return Page of audit logs from specified IP
     */
    Page<AuditLog> findByIpAddress(String ipAddress, Pageable pageable);

    /**
     * Find audit logs for specific user and entity type.
     * Example: All Customer entities modified by a specific admin.
     *
     * @param userId User UUID
     * @param entityType Entity type
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    Page<AuditLog> findByUserIdAndEntityType(UUID userId,
                                             String entityType,
                                             Pageable pageable);


    /**
     * Find audit logs for specific action and date range.
     * Example: All DELETE actions in the past week.
     *
     * @param action Action type
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action = :action " +
            "AND a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByActionAndDateRange(
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Find recent activity for a user.
     * Last N actions performed by a user.
     *
     * @param userId User UUID
     * @param pageable Pagination parameters
     * @return List of recent audit logs
     */
    @Query(value = "SELECT a FROM AuditLog a WHERE a.userId = :userId " +
            "ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentActivityByUser(@Param("userId") UUID userId,
                                            Pageable pageable);

    /**
     * Find failed actions (actions marked as failed in description).
     * Useful for error tracking.
     *
     * @param pageable Pagination parameters
     * @return Page of failed action logs
     */
    @Query("SELECT a FROM AuditLog a WHERE LOWER(a.description) LIKE '%failed%' " +
            "OR LOWER(a.description) LIKE '%error%'")
    Page<AuditLog> findFailedActions(Pageable pageable);

    /**
     * Find suspicious activities (multiple failed attempts from same IP).
     * Security monitoring query.
     *
     * @param threshold Minimum number of failed attempts
     * @param timeWindow Time window to check
     * @return List of suspicious IP addresses with attempt counts
     */
    @Query("SELECT a.ipAddress, COUNT(a) as attemptCount FROM AuditLog a " +
            "WHERE a.createdAt >= :timeWindow " +
            "AND (LOWER(a.description) LIKE '%failed%' OR LOWER(a.description) LIKE '%error%') " +
            "GROUP BY a.ipAddress " +
            "HAVING COUNT(a) >= :threshold")
    List<Object[]> findSuspiciousActivities(
            @Param("threshold") Long threshold,
            @Param("timeWindow") LocalDateTime timeWindow);



    /**
     * Count actions by user and date range.
     * User activity metrics.
     *
     * @param userId User UUID
     * @param startDate Start date
     * @param endDate End date
     * @return Number of actions
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId " +
            "AND a.createdAt BETWEEN :startDate AND :endDate")
    Long countByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Get audit statistics for date range.
     * Returns: total logs, unique users, unique entities, actions per type.
     *
     * @param startDate Start date
     * @param endDate End date
     * @return Object array with statistics
     */
    @Query("SELECT " +
            "COUNT(a), " +
            "COUNT(DISTINCT a.userId), " +
            "COUNT(DISTINCT a.entityId) " +
            "FROM AuditLog a " +
            "WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Object[] getAuditStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find changes to specific entity with old and new values.
     * Complete change history for an entity.
     *
     * @param entityId Entity UUID
     * @return List of audit logs with changes
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityId = :entityId " +
            "AND (a.oldValues IS NOT NULL OR a.newValues IS NOT NULL) " +
            "ORDER BY a.createdAt ASC")
    List<AuditLog> findEntityChangeHistory(@Param("entityId") UUID entityId);

    /**
     * Find all actions by specific user on specific date.
     *
     * @param userId User UUID
     * @param startDate Start of day
     * @param endDate End of day
     * @return List of audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
            "AND a.createdAt >= :startDate " +
            "AND a.createdAt < :endDate " +
            "ORDER BY a.createdAt DESC")
    List<AuditLog> findUserActivityOnDate(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
