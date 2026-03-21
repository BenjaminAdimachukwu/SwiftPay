package com.swiftpay.domain.valueobject;

import com.swiftpay.domain.enums.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object representing monetary amount with currency.
 *
 * Key characteristics of a Value Object:
 * 1. Immutable - once created, cannot be changed
 * 2. Equality by value - two Money objects with same amount+currency are equal
 * 3. Self-validating - invalid money cannot be created
 * 4. No identity - no ID field
 */
public final class Money {

    private final BigDecimal amount;
    private final Currency currency;

    /**
     * Creates a Money value object.
     * Validates immediately - invalid money cannot exist.
     *
     * @param amount   The monetary amount (must be >= 0)
     * @param currency The currency (must not be null)
     */
    public Money(BigDecimal amount, Currency currency) {
        // Validate - fail fast!
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }

        // Scale to 4 decimal places (consistent precision)
        this.amount = amount.setScale(4, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    // -------------------------
    // Static Factory Methods
    // -------------------------

    /**
     * Convenience method: Money.of(100, Currency.USD)
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * Convenience method: Money.of("100.00", Currency.USD)
     */
    public static Money of(String amount, Currency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    /**
     * Creates zero money in specified currency.
     * Useful for initialization.
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // -------------------------
    // Arithmetic Operations
    // -------------------------

    /**
     * Adds two Money amounts.
     * Currencies MUST match.
     *
     * @param other Money to add
     * @return New Money object with sum
     */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Subtracts another Money amount.
     * Currencies MUST match.
     * Result cannot be negative.
     *
     * @param other Money to subtract
     * @return New Money object with difference
     */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Subtraction would result in negative amount: " + result
            );
        }
        return new Money(result, this.currency);
    }

    /**
     * Multiplies amount by a factor.
     * Useful for calculating fees/percentages.
     *
     * Example: money.multiply(new BigDecimal("0.025")) = 2.5% of amount
     *
     * @param factor Multiplication factor
     * @return New Money object with multiplied amount
     */
    public Money multiply(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }

    // -------------------------
    // Comparison Methods
    // -------------------------

    /**
     * Is this amount greater than other?
     */
    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * Is this amount greater than or equal to other?
     */
    public boolean isGreaterThanOrEqual(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    /**
     * Is this amount less than other?
     */
    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * Is this amount zero?
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Is this amount positive (> zero)?
     */
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Do these Money objects have the same currency?
     */
    public boolean isSameCurrency(Money other) {
        return this.currency == other.currency;
    }

    // -------------------------
    // Getters (no setters - immutable!)
    // -------------------------

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    // -------------------------
    // Value Object Equality
    // -------------------------

    /**
     * Equality based on VALUE (amount + currency), not reference.
     * This is the defining characteristic of a Value Object.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0
                && currency == money.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency.getSymbol() + amount.toPlainString();
    }

    // -------------------------
    // Private Helpers
    // -------------------------

    private void requireSameCurrency(Money other) {
        if (this.currency != other.currency) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + this.currency + " vs " + other.currency
            );
        }
    }
}