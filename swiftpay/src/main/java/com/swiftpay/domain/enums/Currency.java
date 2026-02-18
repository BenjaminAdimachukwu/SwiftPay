package com.swiftpay.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum Currency {
    // Major currencies
    USD("US Dollar", "$", 2, true),
    EUR("Euro", "€", 2, true),
    GBP("British Pound", "£", 2, true),
    JPY("Japanese Yen", "¥", 0, true),
    CHF("Swiss Franc", "CHF", 2, true),
    CAD("Canadian Dollar", "C$", 2, true),
    AUD("Australian Dollar", "A$", 2, true),

    // Asian currencies
    CNY("Chinese Yuan", "¥", 2, true),
    INR("Indian Rupee", "₹", 2, true),
    KRW("South Korean Won", "₩", 0, true),
    SGD("Singapore Dollar", "S$", 2, true),
    HKD("Hong Kong Dollar", "HK$", 2, true),

    // African currencies
    NGN("Nigerian Naira", "₦", 2, true);

    private final String displayName;
    private final String symbol;
    private final int decimalPlaces;
    private final boolean isActive;

    public String format(double amount) {
        return String.format("%s %."+decimalPlaces+"f", symbol, amount);
    }

    public boolean usesDecimals() {
        return decimalPlaces > 0;
    }

    public double getMinimumAmount() {
        return decimalPlaces > 0 ? Math.pow(10, -decimalPlaces) : 1.0;
    }

    public double roundAmount(double amount) {
        double multiplier = Math.pow(10, decimalPlaces);
        return Math.round(amount * multiplier) / multiplier;
    }

    public static List<Currency> getActiveCurrencies() {
        return Arrays.stream(values())
                .filter(Currency::isActive)
                .toList();
    }

    public static Currency fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }

        for (Currency currency : Currency.values()) {
            if (currency.name().equalsIgnoreCase(code)) {
                return currency;
            }
        }

        throw new IllegalArgumentException("Invalid currency code: " + code);
    }
}
