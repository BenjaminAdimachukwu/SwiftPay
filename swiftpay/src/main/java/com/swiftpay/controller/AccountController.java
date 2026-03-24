package com.swiftpay.controller;

import com.swiftpay.dto.request.CreateAccountRequest;
import com.swiftpay.dto.response.AccountDTO;
import com.swiftpay.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Account operations.
 *
 * Base URL: /api/accounts
 *
 * Provides endpoints for:
 * - Account creation
 * - Account retrieval (by ID, account number, customer)
 * - Balance management
 * - Account freezing/unfreezing
 * - Credit/debit operations (for testing)
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    // ========================================
    // CREATE Operations
    // ========================================

    /**
     * Create a new account.
     *
     * POST /api/accounts
     *
     * Request body:
     * {
     *   "customerId": "abc-123-def-456",
     *   "accountType": "SAVINGS",
     *   "currency": "USD"
     * }
     *
     * Response: HTTP 201 CREATED with AccountDTO
     *
     * Account number is auto-generated (ACC-XXXXXXXXXX).
     * Initial balance is 0.00.
     *
     * @param request Account creation data
     * @return Created account DTO
     */
    @PostMapping
    public ResponseEntity<AccountDTO> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        log.info("Creating account for customer: {}", request.getCustomerId());

        AccountDTO account = accountService.createAccount(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    // ========================================
    // READ Operations
    // ========================================

    /**
     * Get account by ID.
     *
     * GET /api/accounts/{id}
     *
     * Response: HTTP 200 OK with AccountDTO
     * Error: HTTP 404 if not found
     *
     * @param id Account UUID
     * @return Account DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable UUID id) {

        log.debug("Getting account by ID: {}", id);

        AccountDTO account = accountService.findById(id);

        return ResponseEntity.ok(account);
    }

    /**
     * Get account by account number.
     *
     * GET /api/accounts/number/{accountNumber}
     *
     * Example: GET /api/accounts/number/ACC-1234567890
     *
     * Response: HTTP 200 OK with AccountDTO
     * Error: HTTP 404 if not found
     *
     * @param accountNumber Account number (ACC-XXXXXXXXXX)
     * @return Account DTO
     */
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountDTO> getAccountByNumber(@PathVariable String accountNumber) {

        log.debug("Getting account by number: {}", accountNumber);

        AccountDTO account = accountService.findByAccountNumber(accountNumber);

        return ResponseEntity.ok(account);
    }

    /**
     * Get all accounts for a customer.
     *
     * GET /api/customers/{customerId}/accounts
     *
     * Returns all accounts (SAVINGS, CURRENT, MERCHANT) owned by the customer.
     *
     * Response: HTTP 200 OK with List<AccountDTO>
     * Error: HTTP 404 if customer not found
     *
     * @param customerId Customer UUID
     * @return List of account DTOs
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<AccountDTO>> getCustomerAccounts(@PathVariable UUID customerId) {

        log.debug("Getting accounts for customer: {}", customerId);

        List<AccountDTO> accounts = accountService.findByCustomerId(customerId);

        return ResponseEntity.ok(accounts);
    }

    /**
     * Get account balance information.
     *
     * GET /api/accounts/{id}/balance
     *
     * Returns detailed balance breakdown:
     * - Total balance
     * - Available balance (what can be spent)
     * - Reserved balance (held for pending transactions)
     * - Currency
     *
     * Response: HTTP 200 OK with BalanceInfo
     * Error: HTTP 404 if account not found
     *
     * @param id Account UUID
     * @return Balance information
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<AccountService.BalanceInfo> getBalance(@PathVariable UUID id) {

        log.debug("Getting balance for account: {}", id);

        AccountService.BalanceInfo balance = accountService.getBalance(id);

        return ResponseEntity.ok(balance);
    }

    // ========================================
    // UPDATE Operations
    // ========================================

    /**
     * Freeze an account.
     *
     * POST /api/accounts/{id}/freeze
     *
     * Frozen accounts cannot be used for transactions.
     * Used for security (fraud detection, suspicious activity).
     *
     * Response: HTTP 200 OK with updated AccountDTO
     * Error: HTTP 404 if account not found
     *
     * @param id Account UUID
     * @return Updated account DTO
     */
    @PostMapping("/{id}/freeze")
    public ResponseEntity<AccountDTO> freezeAccount(@PathVariable UUID id) {

        log.warn("Freezing account: {}", id);

        AccountDTO account = accountService.freezeAccount(id);

        return ResponseEntity.ok(account);
    }

    /**
     * Unfreeze an account.
     *
     * POST /api/accounts/{id}/unfreeze
     *
     * Restores account to normal operation.
     * Used after security review or verification.
     *
     * Response: HTTP 200 OK with updated AccountDTO
     * Error: HTTP 404 if account not found
     *
     * @param id Account UUID
     * @return Updated account DTO
     */
    @PostMapping("/{id}/unfreeze")
    public ResponseEntity<AccountDTO> unfreezeAccount(@PathVariable UUID id) {

        log.info("Unfreezing account: {}", id);

        AccountDTO account = accountService.unfreezeAccount(id);

        return ResponseEntity.ok(account);
    }

    // ========================================
    // BALANCE Operations (Testing/Admin Only)
    // ========================================

    /**
     * Credit an account (add money).
     *
     * POST /api/accounts/{id}/credit
     *
     * Request body:
     * {
     *   "amount": 100.00,
     *   "description": "Deposit"
     * }
     *
     * This is for TESTING and ADMIN operations only.
     * Normal credits should go through TransactionService.
     *
     * Response: HTTP 200 OK with updated AccountDTO
     * Error: HTTP 404 if account not found
     * Error: HTTP 403 if account frozen
     * Error: HTTP 400 if amount invalid
     *
     * @param id Account UUID
     * @param request Credit request
     * @return Updated account DTO
     */
    @PostMapping("/{id}/credit")
    public ResponseEntity<AccountDTO> creditAccount(
            @PathVariable UUID id,
            @Valid @RequestBody CreditDebitRequest request) {

        log.info("Crediting account {} with amount: {}", id, request.amount());

        AccountDTO account = accountService.creditAccount(
                id,
                request.amount(),
                request.description()
        );

        return ResponseEntity.ok(account);
    }

    /**
     * Debit an account (remove money).
     *
     * POST /api/accounts/{id}/debit
     *
     * Request body:
     * {
     *   "amount": 50.00,
     *   "description": "Withdrawal"
     * }
     *
     * This is for TESTING and ADMIN operations only.
     * Normal debits should go through TransactionService.
     *
     * Response: HTTP 200 OK with updated AccountDTO
     * Error: HTTP 404 if account not found
     * Error: HTTP 403 if account frozen
     * Error: HTTP 422 if insufficient balance
     * Error: HTTP 400 if amount invalid
     *
     * @param id Account UUID
     * @param request Debit request
     * @return Updated account DTO
     */
    @PostMapping("/{id}/debit")
    public ResponseEntity<AccountDTO> debitAccount(
            @PathVariable UUID id,
            @Valid @RequestBody CreditDebitRequest request) {

        log.info("Debiting account {} with amount: {}", id, request.amount());

        AccountDTO account = accountService.debitAccount(
                id,
                request.amount(),
                request.description()
        );

        return ResponseEntity.ok(account);
    }

    // ========================================
    // Inner Classes (Request DTOs)
    // ========================================

    /**
     * Request DTO for credit/debit operations.
     *
     * Used for testing and admin operations.
     */
    public record CreditDebitRequest(
            @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
            BigDecimal amount,

            String description
    ) {}
}