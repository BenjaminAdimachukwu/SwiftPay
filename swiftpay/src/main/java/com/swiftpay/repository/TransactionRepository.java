package com.swiftpay.repository;

import com.swiftpay.domain.entity.Account;
import com.swiftpay.domain.entity.Transaction;
import com.swiftpay.domain.enums.Currency;
import com.swiftpay.domain.enums.PaymentMethod;
import com.swiftpay.domain.enums.TransactionStatus;
import com.swiftpay.domain.enums.TransactionType;
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

public interface TransactionRepository  extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /**
     * Find transaction by reference number.
     * Transaction reference is unique.
     *
     * @param transactionReference Unique transaction reference
     * @return Optional containing transaction if found
     */
    Optional<Transaction> findByTransactionReference(String transactionReference);

    /**
     * Find transaction by idempotency key.
     * Used to prevent duplicate transaction processing.
     *
     * @param idempotencyKey Unique idempotency key
     * @return Optional containing transaction if found
     */
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    /**
     * Check if transaction with idempotency key exists.
     * Fast existence check without loading full entity.
     *
     * @param idempotencyKey Idempotency key to check
     * @return true if transaction exists
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Find transactions by source account.
     * All outgoing transactions from an account.
     *
     * @param sourceAccount Source account entity
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    Page<Transaction> findBySourceAccount(Account sourceAccount, Pageable pageable);

    /**
     * Find transactions by destination account.
     * All incoming transactions to an account.
     *
     * @param destinationAccount Destination account entity
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    Page<Transaction> findByDestinationAccount(Account destinationAccount, Pageable pageable);


    /**
     * Find all transactions for an account (source or destination).
     * Complete transaction history for an account.
     *
     * @param accountId Account UUID
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.isDeleted = false AND " +
            "(t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)")
    Page<Transaction> findByAccountId(@Param("accountId") UUID accountId, Pageable pageable);


    /** here
     * Find transactions by status.
     *
     * @param status Transaction status
     * @param pageable Pagination parameters
     * @return Page of transactions with specified status
     */
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    /**
     * Find transactions by type.
     *
     * @param transactionType Transaction type
     * @param pageable Pagination parameters
     * @return Page of transactions of specified type
     */
    Page<Transaction> findByTransactionType(TransactionType transactionType, Pageable pageable);

    /**
     * Find transactions by payment method.
     *
     * @param paymentMethod Payment method
     * @param pageable Pagination parameters
     * @return Page of transactions using specified payment method
     */
    Page<Transaction> findByPaymentMethod(PaymentMethod paymentMethod, Pageable pageable);

    /**
     * Find pending transactions.
     * Transactions that are not yet completed or failed.
     *
     * @param pageable Pagination parameters
     * @return Page of pending transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.isDeleted = false AND " +
            "t.status IN ('INITIATED', 'PENDING', 'PROCESSING', 'REQUIRES_VERIFICATION', 'ON_HOLD')")
    Page<Transaction> findPendingTransactions(Pageable pageable);

    /**
     * Find successful transactions.
     *
     * @param pageable Pagination parameters
     * @return Page of successful transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'SUCCESS' AND t.isDeleted = false")
    Page<Transaction> findSuccessfulTransactions(Pageable pageable);

    /**
     * Find failed transactions.
     *
     * @param pageable Pagination parameters
     * @return Page of failed transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'FAILED' AND t.isDeleted = false")
    Page<Transaction> findFailedTransactions(Pageable pageable);

    /**
     * Find transactions by date range.
     *
     * @param startDate Start of date range
     * @param endDate End of date range
     * @param pageable Pagination parameters
     * @return Page of transactions in date range
     */
    Page<Transaction> findByCreatedAtBetween(LocalDateTime startDate,
                                             LocalDateTime endDate,
                                             Pageable pageable);

    /**
     * Find transactions for an account within date range.
     * Useful for account statements.
     *
     * @param accountId Account UUID
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.isDeleted = false AND " +
            "(t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId) AND " +
            "t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Find transactions above amount threshold.
     * Useful for high-value transaction monitoring.
     *
     * @param threshold Minimum amount
     * @param currency Currency to filter
     * @param pageable Pagination parameters
     * @return Page of high-value transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount >= :threshold " +
            "AND t.currency = :currency AND t.isDeleted = false")
    Page<Transaction> findTransactionsAboveAmount(
            @Param("threshold") BigDecimal threshold,
            @Param("currency") Currency currency,
            Pageable pageable
    );

    /**
     * Find child transactions (refunds, reversals) of a parent transaction.
     *
     * @param parentTransactionId Parent transaction UUID
     * @return List of child transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.parentTransaction.id = :parentId " +
            "AND t.isDeleted = false")
    List<Transaction> findByParentTransactionId(@Param("parentId") UUID parentTransactionId);

    /**
     * Find expired transactions that need cleanup.
     * Transactions that have expired but not marked as failed.
     *
     * @param currentTime Current timestamp
     * @return List of expired transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.expiresAt < :currentTime " +
            "AND t.status NOT IN ('SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED') " +
            "AND t.isDeleted = false")
    List<Transaction> findExpiredTransactions(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find transactions stuck in processing.
     * Transactions that have been processing for too long.
     *
     * @param cutoffTime Time before which transactions are considered stuck
     * @return List of stuck transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'PROCESSING' " +
            "AND t.submittedAt < :cutoffTime AND t.isDeleted = false")
    List<Transaction> findStuckTransactions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Calculate total transaction amount for account in date range.
     * Useful for spending analytics.
     *
     * @param accountId Account UUID
     * @param startDate Start date
     * @param endDate End date
     * @param status Transaction status (usually SUCCESS)
     * @return Total amount
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.sourceAccount.id = :accountId " +
            "AND t.status = :status " +
            "AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalAmountByAccountAndDateRange(
            @Param("accountId") UUID accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") TransactionStatus status
    );

    /**
     * Count transactions by account and status.
     *
     * @param accountId Account UUID
     * @param status Transaction status
     * @return Number of transactions
     */
    @Query("SELECT COUNT(t) FROM Transaction t " +
            "WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId) " +
            "AND t.status = :status")
    Long countByAccountIdAndStatus(
            @Param("accountId") UUID accountId,
            @Param("status") TransactionStatus status
    );

    /**
     * Find duplicate transactions by amount, accounts, and time window.
     * Fraud detection: same amount, same accounts, within 5 minutes.
     *
     * @param sourceAccountId Source account UUID
     * @param destinationAccountId Destination account UUID
     * @param amount Transaction amount
     * @param timeWindow Start of time window
     * @return List of potential duplicate transactions
     */
    @Query("SELECT t FROM Transaction t WHERE t.isDeleted = false " +
            "AND t.sourceAccount.id = :sourceAccountId " +
            "AND t.destinationAccount.id = :destinationAccountId " +
            "AND t.amount = :amount " +
            "AND t.createdAt >= :timeWindow")
    List<Transaction> findPotentialDuplicates(
            @Param("sourceAccountId") UUID sourceAccountId,
            @Param("destinationAccountId") UUID destinationAccountId,
            @Param("amount") BigDecimal amount,
            @Param("timeWindow") LocalDateTime timeWindow
    );

    /**
     * Get transaction statistics for a date range.
     * Returns: total count, total amount, avg amount, success count, failed count.
     *
     * @param startDate Start date
     * @param endDate End date
     * @param currency Currency to analyze
     * @return Object array with statistics
     */
    @Query("SELECT " +
            "COUNT(t), " +
            "SUM(t.amount), " +
            "AVG(t.amount), " +
            "SUM(CASE WHEN t.status = 'SUCCESS' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END) " +
            "FROM Transaction t " +
            "WHERE t.createdAt BETWEEN :startDate AND :endDate " +
            "AND t.currency = :currency AND t.isDeleted = false")
    Object[] getTransactionStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("currency") Currency currency
    );

    /**
     * Find transactions by gateway reference.
     * Used for reconciliation with payment gateways.
     *
     * @param gatewayReference Gateway transaction reference
     * @return Optional containing transaction if found
     */
    Optional<Transaction> findByGatewayReference(String gatewayReference);

    /**
     * Find test transactions.
     *
     * @param pageable Pagination parameters
     * @return Page of test transactions
     */
    Page<Transaction> findByIsTestTransaction(Boolean isTestTransaction, Pageable pageable);



}
