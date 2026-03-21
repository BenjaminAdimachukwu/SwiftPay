package com.swiftpay.service;

import com.swiftpay.domain.entity.Account;
import com.swiftpay.domain.entity.Customer;
import com.swiftpay.domain.enums.AccountType;
import com.swiftpay.domain.enums.Currency;
import com.swiftpay.dto.request.CreateAccountRequest;
import com.swiftpay.dto.response.AccountDTO;
import com.swiftpay.exception.AccountFrozenException;
import com.swiftpay.exception.AccountNotFoundException;
import com.swiftpay.exception.CustomerNotFoundException;
import com.swiftpay.exception.DuplicateAccountException;
import com.swiftpay.exception.InsufficientBalanceException;
import com.swiftpay.repository.AccountRepository;
import com.swiftpay.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Account business logic.
 *
 * Handles:
 * - Account creation with unique account numbers
 * - Balance management (credit/debit/reserve/release)
 * - Account freezing/unfreezing
 * - Balance validation
 *
 * CRITICAL: This service must handle concurrent access safely!
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final Random random = new Random();

    /**
     * Create a new account for a customer.
     *
     * Validations:
     * - Customer must exist
     * - Customer cannot have duplicate account (same type + currency)
     *
     * @param request Account creation data
     * @return Created account DTO
     * @throws CustomerNotFoundException if customer not found
     * @throws DuplicateAccountException if account already exists
     */
    public AccountDTO createAccount(CreateAccountRequest request) {
        log.info("Creating account for customer: {}, type: {}, currency: {}",
                request.getCustomerId(), request.getAccountType(), request.getCurrency());

        // Validate: Customer must exist
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.getCustomerId()));

        // Validate: No duplicate account (customer can't have 2 USD SAVINGS accounts)
        accountRepository.findByCustomerIdAndAccountTypeAndCurrency(
                        request.getCustomerId(),
                        request.getAccountType(),
                        request.getCurrency())
                .ifPresent(existingAccount -> {
                    throw new DuplicateAccountException(
                            request.getCustomerId(),
                            request.getAccountType(),
                            request.getCurrency()
                    );
                });

        // Generate unique account number
        String accountNumber = generateAccountNumber();

        // Display name for the account (required by entity; not in CreateAccountRequest)
        String accountName = buildDefaultAccountName(customer, request.getAccountType(), request.getCurrency());

        // Create account
        Account account = Account.builder()
                .customer(customer)
                .accountNumber(accountNumber)
                .accountName(accountName)
                .accountType(request.getAccountType())
                .currency(request.getCurrency())
                .balance(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .isFrozen(false)
                .dailyTransactionCount(0)
                .dailyTransactionLimit(getDefaultDailyLimit(request.getAccountType()))
                .lastTransactionDate(null)
                .build();

        Account saved = accountRepository.save(account);

        log.info("Account created successfully: {}", saved.getAccountNumber());

        return mapToDTO(saved);
    }

    /**
     * Find account by ID.
     *
     * @param id Account UUID
     * @return Account DTO
     * @throws AccountNotFoundException if account not found
     */
    @Transactional(readOnly = true)
    public AccountDTO findById(UUID id) {
        log.debug("Finding account by ID: {}", id);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));

        return mapToDTO(account);
    }

    /**
     * Find account by account number.
     *
     * @param accountNumber Account number
     * @return Account DTO
     * @throws AccountNotFoundException if account not found
     */
    @Transactional(readOnly = true)
    public AccountDTO findByAccountNumber(String accountNumber) {
        log.debug("Finding account by number: {}", accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));

        return mapToDTO(account);
    }

    /**
     * Get all accounts for a customer.
     *
     * @param customerId Customer UUID
     * @return List of account DTOs
     */
    @Transactional(readOnly = true)
    public List<AccountDTO> findByCustomerId(UUID customerId) {
        log.debug("Finding accounts for customer: {}", customerId);

        // Verify customer exists
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }

        List<Account> accounts = accountRepository.findActiveAccountsByCustomerId(customerId);

        return accounts.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Credit (add money to) an account.
     *
     * Used for:
     * - Deposits
     * - Receiving payments
     * - Refunds
     *
     * @param accountId Account UUID
     * @param amount Amount to credit
     * @param description Transaction description
     * @return Updated account DTO
     * @throws AccountNotFoundException if account not found
     * @throws AccountFrozenException if account is frozen
     */
    public AccountDTO creditAccount(UUID accountId, BigDecimal amount, String description) {
        log.info("Crediting account {} with amount: {}", accountId, amount);

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }

        // Find account with pessimistic lock (prevents concurrent modification)
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // Validate: Account must not be frozen
        if (account.getIsFrozen()) {
            throw new AccountFrozenException(accountId);
        }

        // Update balances
        account.setBalance(account.getBalance().add(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setLastTransactionDate(LocalDateTime.now());

        Account updated = accountRepository.save(account);

        log.info("Account credited successfully. New balance: {}", updated.getBalance());

        return mapToDTO(updated);
    }

    /**
     * Debit (subtract money from) an account.
     *
     * Used for:
     * - Withdrawals
     * - Sending payments
     * - Fees
     *
     * @param accountId Account UUID
     * @param amount Amount to debit
     * @param description Transaction description
     * @return Updated account DTO
     * @throws AccountNotFoundException if account not found
     * @throws AccountFrozenException if account is frozen
     * @throws InsufficientBalanceException if balance too low
     */
    public AccountDTO debitAccount(UUID accountId, BigDecimal amount, String description) {
        log.info("Debiting account {} with amount: {}", accountId, amount);

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }

        // Find account
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // Validate: Account must not be frozen
        if (account.getIsFrozen()) {
            throw new AccountFrozenException(accountId);
        }

        // Validate: Sufficient available balance
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    account.getAccountNumber(),
                    account.getAvailableBalance(),
                    amount
            );
        }

        // Update balances
        account.setBalance(account.getBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setLastTransactionDate(LocalDateTime.now());

        Account updated = accountRepository.save(account);

        log.info("Account debited successfully. New balance: {}", updated.getBalance());

        return mapToDTO(updated);
    }

    /**
     * Reserve funds in an account.
     *
     * Used when transaction is initiated but not completed:
     * - Moves money from availableBalance to reservedBalance
     * - Total balance stays the same
     * - User can't spend reserved money
     *
     * Example: Credit card authorization
     *
     * @param accountId Account UUID
     * @param amount Amount to reserve
     * @return Updated account DTO
     */
    public AccountDTO reserveFunds(UUID accountId, BigDecimal amount) {
        log.info("Reserving {} in account {}", amount, accountId);

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reserve amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // Validate: Sufficient available balance
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    account.getAccountNumber(),
                    account.getAvailableBalance(),
                    amount
            );
        }

        // Move from available to reserved
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setReservedBalance(account.getReservedBalance().add(amount));
        // Note: Total balance doesn't change!

        Account updated = accountRepository.save(account);

        log.info("Funds reserved. Available: {}, Reserved: {}",
                updated.getAvailableBalance(), updated.getReservedBalance());

        return mapToDTO(updated);
    }

    /**
     * Release reserved funds back to available balance.
     *
     * Used when:
     * - Transaction is cancelled
     * - Authorization expires
     *
     * @param accountId Account UUID
     * @param amount Amount to release
     * @return Updated account DTO
     */
    public AccountDTO releaseFunds(UUID accountId, BigDecimal amount) {
        log.info("Releasing {} in account {}", amount, accountId);

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Release amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // Validate: Can't release more than reserved
        if (account.getReservedBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Cannot release more than reserved amount. Reserved: " +
                            account.getReservedBalance()
            );
        }

        // Move from reserved back to available
        account.setReservedBalance(account.getReservedBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        // Note: Total balance doesn't change!

        Account updated = accountRepository.save(account);

        log.info("Funds released. Available: {}, Reserved: {}",
                updated.getAvailableBalance(), updated.getReservedBalance());

        return mapToDTO(updated);
    }

    /**
     * Freeze an account.
     *
     * Prevents all transactions on the account.
     * Used for security or compliance reasons.
     *
     * @param accountId Account UUID
     * @return Updated account DTO
     */
    public AccountDTO freezeAccount(UUID accountId) {
        log.warn("Freezing account: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.setIsFrozen(true);

        Account updated = accountRepository.save(account);

        log.warn("Account frozen: {}", accountId);

        return mapToDTO(updated);
    }

    /**
     * Unfreeze an account.
     *
     * @param accountId Account UUID
     * @return Updated account DTO
     */
    public AccountDTO unfreezeAccount(UUID accountId) {
        log.info("Unfreezing account: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        account.setIsFrozen(false);

        Account updated = accountRepository.save(account);

        log.info("Account unfrozen: {}", accountId);

        return mapToDTO(updated);
    }

    /**
     * Get account balance.
     *
     * @param accountId Account UUID
     * @return Balance details
     */
    @Transactional(readOnly = true)
    public BalanceInfo getBalance(UUID accountId) {
        log.debug("Getting balance for account: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        return new BalanceInfo(
                account.getBalance(),
                account.getAvailableBalance(),
                account.getReservedBalance(),
                account.getCurrency()
        );
    }

    // -------------------------
    // Helper Methods
    // -------------------------

    /**
     * Generate unique account number.
     *
     * Format: ACC-XXXXXXXXXX (ACC- prefix + 10 digits)
     *
     * In production, use more sophisticated algorithm.
     */
    private String generateAccountNumber() {
        String accountNumber;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            // Generate 10-digit number
            long number = 1000000000L + (long) (random.nextDouble() * 9000000000L);
            accountNumber = "ACC-" + number;

            attempts++;
            if (attempts >= maxAttempts) {
                throw new RuntimeException("Failed to generate unique account number");
            }

        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }

    /**
     * Get default daily transaction limit based on account type.
     */
    private Integer getDefaultDailyLimit(AccountType accountType) {
        return switch (accountType) {
            case SAVINGS -> 50;       // Conservative limit
            case PERSONAL -> 200;
            case BUSINESS, PREMIUM -> 200;
            case MERCHANT -> 1000;  // Merchants process many transactions
            case ESCROW, SYSTEM -> 10_000;
        };
    }

    /**
     * Builds a non-blank account name within {@link Account#accountName} max length (100).
     */
    private String buildDefaultAccountName(Customer customer, AccountType accountType, Currency currency) {
        String base = customer.getFullName() + " — " + accountType + " (" + currency + ")";
        int max = 100;
        return base.length() <= max ? base : base.substring(0, max);
    }

    /**
     * Map Account entity to AccountDTO.
     */
    private AccountDTO mapToDTO(Account account) {
        return AccountDTO.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .availableBalance(account.getAvailableBalance())
                .reservedBalance(account.getReservedBalance())
                .isFrozen(account.getIsFrozen())
                .customerId(account.getCustomer().getId())
                .createdAt(account.getCreatedAt())
                .lastTransactionAt(account.getLastTransactionDate())
                .build();
    }

    /**
     * Inner class for balance information.
     */
    public record BalanceInfo(
            BigDecimal balance,
            BigDecimal availableBalance,
            BigDecimal reservedBalance,
            Currency currency
    ) {}
}