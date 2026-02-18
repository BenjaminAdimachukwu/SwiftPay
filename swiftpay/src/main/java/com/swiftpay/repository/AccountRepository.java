package com.swiftpay.repository;

import com.swiftpay.domain.entity.Account;
import com.swiftpay.domain.entity.Customer;
import com.swiftpay.domain.enums.AccountType;
import com.swiftpay.domain.enums.Currency;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

    /**
     * Find account by account number.
     * Account number is unique across the system.
     *
     * @param accountNumber Unique account number
     * @return Optional containing account if found
     */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Find account by account number with pessimistic lock.
     * Used during transactions to prevent concurrent modifications.
     *
     * @param accountNumber Unique account number
     * @return Optional containing locked account if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    /**
     * Check if account number already exists.
     *
     * @param accountNumber Account number to check
     * @return true if account number exists
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Find all accounts belonging to a customer.
     *
     * @param customer Customer entity
     * @param pageable Pagination parameters
     * @return Page of customer's accounts
     */
    Page<Account> findByCustomer(Customer customer, Pageable pageable);

    /**
     * Find accounts by customer ID.
     *
     * @param customerId Customer's UUID
     * @param pageable Pagination parameters
     * @return Page of accounts
     */
    Page<Account> findByCustomerId(UUID customerId, Pageable pageable);
    /**
     * Find customer's accounts by type.
     * Example: Find all SAVINGS accounts for a customer.
     *
     * @param customerId Customer's UUID
     * @param accountType Type of account
     * @return List of matching accounts
     */
    List<Account> findByCustomerIdAndAccountType(UUID customerId, AccountType accountType);

    /**
     * Find customer's account by type and currency.
     * Most specific query - returns single account.
     *
     * @param customerId Customer's UUID
     * @param accountType Type of account
     * @param currency Account currency
     * @return Optional containing matching account
     */
    Optional<Account> findByCustomerIdAndAccountTypeAndCurrency(
            UUID customerId,
            AccountType accountType,
            Currency currency
    );

    /**
     * Find accounts by currency.
     *
     * @param currency Currency code
     * @param pageable Pagination parameters
     * @return Page of accounts in specified currency
     */
    Page<Account> findByCurrency(Currency currency, Pageable pageable);

    /**
     * Find accounts by type.
     *
     * @param accountType Account type
     * @param pageable Pagination parameters
     * @return Page of accounts of specified type
     */
    Page<Account> findByAccountType(AccountType accountType, Pageable pageable);

    /**
     * Find frozen accounts.
     * Useful for compliance and security monitoring.
     *
     * @param isFrozen Frozen status
     * @param pageable Pagination parameters
     * @return Page of frozen accounts
     */
    Page<Account> findByIsFrozen(Boolean isFrozen, Pageable pageable);

    /**
     * Find active accounts (not deleted, not frozen).
     *
     * @param customerId Customer's UUID
     * @return List of active accounts
     */
    @Query("SELECT a FROM Account a WHERE a.customer.id = :customerId " +
            "AND a.isDeleted = false AND a.isFrozen = false")
    List<Account> findActiveAccountsByCustomerId(@Param("customerId") UUID customerId);
    /**
     * Find accounts with balance above threshold.
     * Useful for high-value account monitoring.
     *
     * @param threshold Minimum balance
     * @param currency Currency to filter by
     * @param pageable Pagination parameters
     * @return Page of high-balance accounts
     */
    @Query("SELECT a FROM Account a WHERE a.balance >= :threshold " +
            "AND a.currency = :currency AND a.isDeleted = false")
    Page<Account> findAccountsWithBalanceAbove(
            @Param("threshold") BigDecimal threshold,
            @Param("currency") Currency currency,
            Pageable pageable
    );
    /**
     * Find accounts with low balance (below threshold).
     * Useful for low balance alerts.
     *
     * @param threshold Maximum balance
     * @param currency Currency to filter by
     * @return List of low-balance accounts
     */

    @Query("SELECT a FROM Account a WHERE a.balance < :threshold " +
            "AND a.currency = :currency AND a.isDeleted = false")
    List<Account> findAccountsWithLowBalance(
            @Param("threshold") BigDecimal threshold,
            @Param("currency") Currency currency
    );

    /**
     * Find accounts with reserved balance.
     * Accounts with funds on hold.
     *
     * @param pageable Pagination parameters
     * @return Page of accounts with reserved balance
     */
    @Query("SELECT a FROM Account a WHERE a.reservedBalance > 0 AND a.isDeleted = false")
    Page<Account> findAccountsWithReservedBalance(Pageable pageable);

    /**
     * Calculate total balance across all accounts for a customer.
     * Useful for net worth calculation.
     *
     * @param customerId Customer's UUID
     * @param currency Currency to sum
     * @return Total balance in specified currency
     */
    @Query("SELECT SUM(a.balance) FROM Account a " +
            "WHERE a.customer.id = :customerId " +
            "AND a.currency = :currency " +
            "AND a.isDeleted = false")
    BigDecimal calculateTotalBalanceByCustomerAndCurrency(
            @Param("customerId") UUID customerId,
            @Param("currency") Currency currency
    );

    /**
     * Count accounts by type and currency.
     * Useful for analytics and reporting.
     *
     * @param accountType Account type
     * @param currency Currency
     * @return Number of accounts
     */
    @Query("SELECT COUNT(a) FROM Account a " +
            "WHERE a.accountType = :accountType " +
            "AND a.currency = :currency " +
            "AND a.isDeleted = false")
    Long countByAccountTypeAndCurrency(
            @Param("accountType") AccountType accountType,
            @Param("currency") Currency currency
    );

    /**
     * Find accounts exceeding daily transaction limit.
     * Useful for fraud detection.
     *
     * @return List of accounts that may need review
     */
    @Query("SELECT a FROM Account a " +
            "WHERE a.dailyTransactionCount > a.dailyTransactionLimit " +
            "AND a.isDeleted = false")
    List<Account> findAccountsExceedingDailyLimit();

    /**
     * Get account summary statistics by currency.
     * Returns: total accounts, total balance, average balance.
     *
     * @param currency Currency to analyze
     * @return Object array with statistics
     */
    @Query("SELECT " +
            "COUNT(a), " +
            "SUM(a.balance), " +
            "AVG(a.balance) " +
            "FROM Account a " +
            "WHERE a.currency = :currency AND a.isDeleted = false")
    Object[] getAccountStatisticsByCurrency(@Param("currency") Currency currency);

    /**
     * Find accounts that need balance reconciliation.
     * Where balance != availableBalance + reservedBalance.
     *
     * @return List of accounts with balance discrepancies
     */
    @Query("SELECT a FROM Account a WHERE a.isDeleted = false " +
            "AND a.balance != (a.availableBalance + a.reservedBalance)")
    List<Account> findAccountsWithBalanceDiscrepancies();


}
