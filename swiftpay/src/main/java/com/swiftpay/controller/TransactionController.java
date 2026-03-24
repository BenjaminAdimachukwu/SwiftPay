package com.swiftpay.controller;

import com.swiftpay.dto.request.CreateTransactionRequest;
import com.swiftpay.dto.response.TransactionDTO;
import com.swiftpay.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Transaction operations.
 *
 * Base URL: /api/transactions
 *
 * This is the CORE of the payment system!
 *
 * Provides endpoints for:
 * - Payment processing (debit source, credit destination)
 * - Transaction retrieval (by ID, reference, account)
 * - Transaction history with pagination
 * - Pending transaction management
 * - Transaction cancellation
 *
 * All payment processing goes through TransactionService which handles:
 * - Idempotency (prevent duplicate payments)
 * - Balance validation
 * - Account locking (pessimistic locks)
 * - Double-entry bookkeeping
 * - Fee calculation
 * - Transaction status management
 * - Rollback on failure
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    // ========================================
    // CREATE Operations
    // ========================================

    /**
     * Process a payment transaction.
     *
     * POST /api/transactions
     *
     * This is the MAIN endpoint for processing payments!
     *
     * Request body:
     * {
     *   "idempotencyKey": "unique-key-123",  // Optional - server generates if not provided
     *   "transactionType": "PAYMENT",
     *   "paymentMethod": "CARD",
     *   "amount": 100.00,
     *   "currency": "USD",
     *   "sourceAccountId": "abc-123-def-456",
     *   "destinationAccountId": "xyz-789-ghi-012",
     *   "description": "Payment for invoice #1234",
     *   "metadata": "{ \"orderId\": \"12345\" }"
     * }
     *
     * Response: HTTP 201 CREATED with TransactionDTO
     *
     * The service will:
     * 1. Check idempotency (return existing if duplicate)
     * 2. Validate accounts (exist, not frozen, sufficient balance)
     * 3. Lock accounts (pessimistic lock to prevent race conditions)
     * 4. Calculate fees
     * 5. Debit source account
     * 6. Credit destination account
     * 7. Create transaction record
     * 8. Commit or rollback everything atomically
     *
     * Error responses:
     * - HTTP 404: Account not found
     * - HTTP 403: Account frozen
     * - HTTP 422: Insufficient balance
     * - HTTP 409: Duplicate transaction (idempotency key exists)
     * - HTTP 400: Invalid request (validation errors)
     *
     * @param request Transaction creation data
     * @return Created transaction DTO
     */
    @PostMapping
    public ResponseEntity<TransactionDTO> processPayment(
            @Valid @RequestBody CreateTransactionRequest request) {

        log.info("Processing payment: {} {} from {} to {}",
                request.getAmount(),
                request.getCurrency(),
                request.getSourceAccountId(),
                request.getDestinationAccountId());

        TransactionDTO transaction = transactionService.processPayment(request);

        log.info("Payment processed successfully: {}", transaction.getTransactionReference());

        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    // ========================================
    // READ Operations
    // ========================================

    /**
     * Get transaction by ID.
     *
     * GET /api/transactions/{id}
     *
     * Response: HTTP 200 OK with TransactionDTO
     * Error: HTTP 404 if not found
     *
     * @param id Transaction UUID
     * @return Transaction DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable UUID id) {

        log.debug("Getting transaction by ID: {}", id);

        TransactionDTO transaction = transactionService.findById(id);

        return ResponseEntity.ok(transaction);
    }

    /**
     * Get transaction by reference.
     *
     * GET /api/transactions/reference/{transactionReference}
     *
     * Transaction reference format: TXN-YYYYMMDD-XXXXXXXX
     * Example: TXN-20260323-A1B2C3D4
     *
     * Response: HTTP 200 OK with TransactionDTO
     * Error: HTTP 404 if not found
     *
     * @param transactionReference Transaction reference
     * @return Transaction DTO
     */
    @GetMapping("/reference/{transactionReference}")
    public ResponseEntity<TransactionDTO> getTransactionByReference(
            @PathVariable String transactionReference) {

        log.debug("Getting transaction by reference: {}", transactionReference);

        TransactionDTO transaction = transactionService.findByReference(transactionReference);

        return ResponseEntity.ok(transaction);
    }

    /**
     * Get transaction history for an account.
     *
     * GET /api/accounts/{accountId}/transactions?page=0&size=20&sort=initiatedAt,desc
     *
     * Returns all transactions where the account is either source or destination.
     *
     * Query parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     * - sort: Sort field and direction (default: initiatedAt,desc - newest first)
     *
     * Response: HTTP 200 OK with Page<TransactionDTO>
     * Error: HTTP 404 if account not found
     *
     * Example response:
     * {
     *   "content": [
     *     { "id": "...", "amount": 100.00, "status": "SUCCESS", ... },
     *     { "id": "...", "amount": 50.00, "status": "SUCCESS", ... }
     *   ],
     *   "pageable": { "pageNumber": 0, "pageSize": 20 },
     *   "totalElements": 150,
     *   "totalPages": 8,
     *   "last": false
     * }
     *
     * @param accountId Account UUID
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field (default: initiatedAt)
     * @param sortDir Sort direction (default: desc)
     * @return Page of transaction DTOs
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionDTO>> getAccountTransactions(
            @PathVariable UUID accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "initiatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.debug("Getting transactions for account: {}, page: {}", accountId, page);

        // Create sort
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // Create pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TransactionDTO> transactions = transactionService.findByAccountId(accountId, pageable);

        return ResponseEntity.ok(transactions);
    }

    /**
     * Get all pending transactions.
     *
     * GET /api/transactions/pending?page=0&size=20
     *
     * Returns transactions with status INITIATED or PENDING.
     *
     * Useful for:
     * - Admin dashboard (see stuck transactions)
     * - Background job processing (retry failed transactions)
     * - Monitoring and alerting
     *
     * Query parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 20)
     *
     * Response: HTTP 200 OK with Page<TransactionDTO>
     *
     * @param page Page number
     * @param size Page size
     * @return Page of pending transaction DTOs
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<TransactionDTO>> getPendingTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Getting pending transactions, page: {}", page);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "initiatedAt"));

        Page<TransactionDTO> transactions = transactionService.findPendingTransactions(pageable);

        return ResponseEntity.ok(transactions);
    }

    // ========================================
    // UPDATE Operations
    // ========================================

    /**
     * Cancel a pending transaction.
     *
     * POST /api/transactions/{id}/cancel
     *
     * Only works if transaction status is INITIATED or PENDING.
     * Cannot cancel completed transactions (SUCCESS or FAILED).
     *
     * Use cases:
     * - User cancels payment before completion
     * - Admin cancels suspicious transaction
     * - Timeout handling (cancel if taking too long)
     *
     * Response: HTTP 200 OK with updated TransactionDTO
     * Error: HTTP 404 if transaction not found
     * Error: HTTP 400 if transaction already completed
     *
     * @param id Transaction UUID
     * @return Updated transaction DTO (status = CANCELLED)
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TransactionDTO> cancelTransaction(@PathVariable UUID id) {

        log.warn("Cancelling transaction: {}", id);

        TransactionDTO transaction = transactionService.cancelTransaction(id);

        log.info("Transaction cancelled: {}", transaction.getTransactionReference());

        return ResponseEntity.ok(transaction);
    }
}