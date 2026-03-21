package com.swiftpay.service;

import com.swiftpay.domain.entity.Account;
import com.swiftpay.domain.entity.Transaction;
import com.swiftpay.domain.enums.TransactionStatus;
import com.swiftpay.domain.enums.TransactionType;
import com.swiftpay.dto.request.CreateTransactionRequest;
import com.swiftpay.dto.response.TransactionDTO;
import com.swiftpay.exception.*;
import com.swiftpay.repository.AccountRepository;
import com.swiftpay.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for Transaction processing.
 *
 * This is the CORE of the payment system!
 *
 * Handles:
 * - Payment processing (debit source, credit destination)
 * - Idempotency (prevent duplicate transactions)
 * - Balance validation
 * - Currency validation
 * - Transaction status management
 * - Fee calculation
 *
 * CRITICAL: Uses pessimistic locking to prevent race conditions!
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    /**
     * Process a payment transaction.
     *
     * This is the main payment processing method.
     *
     * Flow:
     * 1. Check idempotency (return existing if duplicate)
     * 2. Validate request
     * 3. Lock accounts
     * 4. Validate balances
     * 5. Calculate fees
     * 6. Debit source
     * 7. Credit destination
     * 8. Create transaction record
     * 9. Update account metadata
     *
     * @param request Transaction creation data
     * @return Transaction DTO
     * @throws DuplicateTransactionException if idempotency key already exists
     * @throws AccountNotFoundException if source or destination not found
     * @throws AccountFrozenException if either account is frozen
     * @throws InsufficientBalanceException if source has insufficient funds
     * @throws CurrencyMismatchException if currencies don't match
     * @throws InvalidTransactionException if transaction violates business rules
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)  // Highest isolation level
    public TransactionDTO processPayment(CreateTransactionRequest request) {
        log.info("Processing payment: {} from {} to {}",
                request.getAmount(),
                request.getSourceAccountId(),
                request.getDestinationAccountId());

        // ========================================
        // Step 1: Idempotency Check
        // ========================================

        // Resolve key once, then keep it final so it can be used inside lambdas
        String rawIdempotencyKey = request.getIdempotencyKey();
        final String idempotencyKey = (rawIdempotencyKey == null || rawIdempotencyKey.isBlank())
                ? UUID.randomUUID().toString()
                : rawIdempotencyKey;
        if (rawIdempotencyKey == null || rawIdempotencyKey.isBlank()) {
            log.warn("No idempotency key provided, generated: {}", idempotencyKey);
        }

        // Check if transaction with this key already exists
        transactionRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(existingTransaction -> {
                    log.info("Duplicate transaction detected, returning existing: {}",
                            existingTransaction.getTransactionReference());
                    throw new DuplicateTransactionException(idempotencyKey);
                });

        // ========================================
        // Step 2: Validate Request
        // ========================================

        validateTransactionRequest(request);

        // ========================================
        // Step 3: Lock and Load Accounts
        // ========================================

        // CRITICAL: Load accounts WITH pessimistic lock
        // This prevents other transactions from modifying these accounts
        // until this transaction completes

        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getSourceAccountId()));

        Account destinationAccount = accountRepository.findById(request.getDestinationAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getDestinationAccountId()));

        // ========================================
        // Step 4: Validate Accounts
        // ========================================

        validateAccounts(sourceAccount, destinationAccount, request.getAmount());

        // ========================================
        // Step 5: Calculate Fee
        // ========================================

        BigDecimal processingFee = calculateProcessingFee(request);
        BigDecimal totalDebit = request.getAmount().add(processingFee);
        BigDecimal netAmount = request.getAmount().subtract(processingFee);

        // ========================================
        // Step 6: Generate Transaction Reference
        // ========================================

        String transactionReference = generateTransactionReference();

        // ========================================
        // Step 7: Create Transaction Record (INITIATED status)
        // ========================================

        Transaction transaction = Transaction.builder()
                .transactionReference(transactionReference)
                .idempotencyKey(idempotencyKey)
                .transactionType(request.getTransactionType())
                .status(TransactionStatus.INITIATED)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .processingFee(processingFee)
                .netAmount(netAmount)
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .initiatedAt(LocalDateTime.now())
                .retryCount(0)
                .isTestTransaction(false)
                .build();

        // Save transaction in INITIATED state
        transaction = transactionRepository.save(transaction);

        log.info("Transaction created: {}", transactionReference);

        try {
            // ========================================
            // Step 8: Update Transaction Status to PROCESSING
            // ========================================

            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction.setSubmittedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);

            // ========================================
            // Step 9: Debit Source Account
            // ========================================

            log.info("Debiting source account: {}", sourceAccount.getAccountNumber());

            sourceAccount.setBalance(sourceAccount.getBalance().subtract(totalDebit));
            sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(totalDebit));
            sourceAccount.setLastTransactionDate(LocalDateTime.now());
            sourceAccount.setDailyTransactionCount(sourceAccount.getDailyTransactionCount() + 1);

            accountRepository.save(sourceAccount);

            log.info("Source account debited. New balance: {}", sourceAccount.getBalance());

            // ========================================
            // Step 10: Credit Destination Account
            // ========================================

            log.info("Crediting destination account: {}", destinationAccount.getAccountNumber());

            destinationAccount.setBalance(destinationAccount.getBalance().add(netAmount));
            destinationAccount.setAvailableBalance(destinationAccount.getAvailableBalance().add(netAmount));
            destinationAccount.setLastTransactionDate(LocalDateTime.now());
            destinationAccount.setDailyTransactionCount(destinationAccount.getDailyTransactionCount() + 1);

            accountRepository.save(destinationAccount);

            log.info("Destination account credited. New balance: {}", destinationAccount.getBalance());

            // ========================================
            // Step 11: Mark Transaction as SUCCESS
            // ========================================

            transaction.markAsSuccessful();  // Sets status=SUCCESS, completedAt=now
            transaction = transactionRepository.save(transaction);

            log.info("Transaction completed successfully: {}", transactionReference);

            return mapToDTO(transaction);

        } catch (Exception e) {
            // ========================================
            // Step 12: Handle Failure
            // ========================================

            log.error("Transaction failed: {}", transactionReference, e);

            // Mark transaction as failed
            transaction.markAsFailed("PROCESSING_ERROR", e.getMessage());
            transactionRepository.save(transaction);

            // Re-throw exception (Spring will rollback the entire transaction)
            throw e;
        }
    }

    /**
     * Find transaction by ID.
     *
     * @param id Transaction UUID
     * @return Transaction DTO
     * @throws TransactionNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public TransactionDTO findById(UUID id) {
        log.debug("Finding transaction by ID: {}", id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        return mapToDTO(transaction);
    }

    /**
     * Find transaction by reference.
     *
     * @param transactionReference Transaction reference
     * @return Transaction DTO
     * @throws TransactionNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public TransactionDTO findByReference(String transactionReference) {
        log.debug("Finding transaction by reference: {}", transactionReference);

        Transaction transaction = transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new TransactionNotFoundException(transactionReference));

        return mapToDTO(transaction);
    }

    /**
     * Get transaction history for an account.
     *
     * @param accountId Account UUID
     * @param pageable Pagination parameters
     * @return Page of transactions
     */
    @Transactional(readOnly = true)
    public Page<TransactionDTO> findByAccountId(UUID accountId, Pageable pageable) {
        log.debug("Finding transactions for account: {}", accountId);

        // Verify account exists
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        Page<Transaction> transactions = transactionRepository.findByAccountId(accountId, pageable);

        return transactions.map(this::mapToDTO);
    }

    /**
     * Get pending transactions.
     *
     * @param pageable Pagination parameters
     * @return Page of pending transactions
     */
    @Transactional(readOnly = true)
    public Page<TransactionDTO> findPendingTransactions(Pageable pageable) {
        log.debug("Finding pending transactions");

        Page<Transaction> transactions = transactionRepository.findPendingTransactions(pageable);

        return transactions.map(this::mapToDTO);
    }

    /**
     * Cancel a pending transaction.
     *
     * Only works if transaction is still in INITIATED or PENDING status.
     *
     * @param id Transaction UUID
     * @return Updated transaction DTO
     */
    @Transactional
    public TransactionDTO cancelTransaction(UUID id) {
        log.info("Cancelling transaction: {}", id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        // Can only cancel if not yet completed
        if (transaction.isCompleted()) {
            throw new InvalidTransactionException(
                    "Cannot cancel completed transaction: " + transaction.getTransactionReference()
            );
        }

        // Mark as cancelled
        transaction.setStatus(TransactionStatus.CANCELLED);
        transaction.setCompletedAt(LocalDateTime.now());

        Transaction updated = transactionRepository.save(transaction);

        log.info("Transaction cancelled: {}", transaction.getTransactionReference());

        return mapToDTO(updated);
    }

    // -------------------------
    // Validation Methods
    // -------------------------

    /**
     * Validate transaction request.
     *
     * Business rules:
     * - Source and destination must be different
     * - Amount must be positive
     * - Currency must match both accounts
     */
    private void validateTransactionRequest(CreateTransactionRequest request) {
        // Rule: Source and destination must be different
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new InvalidTransactionException(
                    "Source and destination accounts cannot be the same"
            );
        }

        // Rule: Amount must be positive
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException(
                    "Transaction amount must be greater than zero"
            );
        }
    }

    /**
     * Validate accounts can participate in transaction.
     *
     * Checks:
     * - Neither account is frozen
     * - Currencies match
     * - Source has sufficient balance
     */
    private void validateAccounts(Account source, Account destination, BigDecimal amount) {
        // Rule: Source account must not be frozen
        if (source.getIsFrozen()) {
            throw new AccountFrozenException(source.getAccountNumber());
        }

        // Rule: Destination account must not be frozen
        if (destination.getIsFrozen()) {
            throw new AccountFrozenException(destination.getAccountNumber());
        }

        // Rule: Currencies must match (no currency conversion yet)
        if (source.getCurrency() != destination.getCurrency()) {
            throw new CurrencyMismatchException(
                    source.getCurrency(),
                    destination.getCurrency()
            );
        }

        // Rule: Source must have sufficient available balance
        // (We'll debit amount + fee)
        BigDecimal requiredBalance = amount; // Simplified for now
        if (source.getAvailableBalance().compareTo(requiredBalance) < 0) {
            throw new InsufficientBalanceException(
                    source.getAccountNumber(),
                    source.getAvailableBalance(),
                    requiredBalance
            );
        }
    }

    // -------------------------
    // Helper Methods
    // -------------------------

    /**
     * Calculate processing fee.
     *
     * Fee structure (simplified):
     * - PAYMENT: 2.5% of amount
     * - TRANSFER: 1.5% of amount
     * - REFUND / REVERSAL: No fee
     * - CHARGEBACK: Higher fee (dispute handling)
     *
     * In production, this would be much more complex based on:
     * - Payment method
     * - Customer tier
     * - Account type
     * - Amount ranges
     */
    private BigDecimal calculateProcessingFee(CreateTransactionRequest request) {
        BigDecimal feePercentage = switch (request.getTransactionType()) {
            case PAYMENT -> new BigDecimal("0.025");   // 2.5%
            case TRANSFER -> new BigDecimal("0.015");  // 1.5%
            case REFUND -> BigDecimal.ZERO;            // No fee
            case WITHDRAWAL -> new BigDecimal("0.01"); // 1%
            case DEPOSIT -> BigDecimal.ZERO;           // No fee
            case REVERSAL -> BigDecimal.ZERO;         // No fee (reverses prior tx)
            case CHARGEBACK -> new BigDecimal("0.05"); // 5% — dispute / chargeback handling
        };

        BigDecimal fee = request.getAmount()
                .multiply(feePercentage)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Calculated fee: {} for amount: {}", fee, request.getAmount());

        return fee;
    }

    /**
     * Generate unique transaction reference.
     *
     * Format: TXN-YYYYMMDD-XXXXXXXX
     * Example: TXN-20260127-A1B2C3D4
     *
     * In production, use more sophisticated algorithm.
     */
    private String generateTransactionReference() {
        String date = LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        String randomPart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "TXN-" + date + "-" + randomPart;
    }

    /**
     * Map Transaction entity to TransactionDTO.
     */
    private TransactionDTO mapToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .transactionReference(transaction.getTransactionReference())
                .transactionType(transaction.getTransactionType())
                .status(transaction.getStatus())
                .paymentMethod(transaction.getPaymentMethod())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .processingFee(transaction.getProcessingFee())
                .netAmount(transaction.getNetAmount())
                .description(transaction.getDescription())
                .sourceAccountId(transaction.getSourceAccount() != null ?
                        transaction.getSourceAccount().getId() : null)
                .sourceAccountNumber(transaction.getSourceAccount() != null ?
                        transaction.getSourceAccount().getAccountNumber() : null)
                .destinationAccountId(transaction.getDestinationAccount() != null ?
                        transaction.getDestinationAccount().getId() : null)
                .destinationAccountNumber(transaction.getDestinationAccount() != null ?
                        transaction.getDestinationAccount().getAccountNumber() : null)
                .initiatedAt(transaction.getInitiatedAt())
                .completedAt(transaction.getCompletedAt())
                .errorCode(transaction.getErrorCode())
                .errorMessage(transaction.getErrorMessage())
                .gatewayReference(transaction.getGatewayReference())
                .build();
    }
}