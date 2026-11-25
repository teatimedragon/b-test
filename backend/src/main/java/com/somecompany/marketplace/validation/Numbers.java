package com.somecompany.marketplace.validation;

import java.math.BigDecimal;
import java.util.Optional;

/** Strict number parsing for raw string values. Rejects NaN, Infinity and anything non-numeric. */
public final class Numbers {

    private Numbers() {
    }

    public static Optional<BigDecimal> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static boolean isWhole(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 0;
    }

    /** Digits after the decimal point, ignoring trailing zeros ({@code 2.50} has 1). */
    public static int fractionDigits(BigDecimal value) {
        return Math.max(0, value.stripTrailingZeros().scale());
    }

    /** Digits before the decimal point ({@code 0.5} has 0, {@code 450000} has 6). */
    public static int integerDigits(BigDecimal value) {
        BigDecimal abs = value.abs().stripTrailingZeros();
        return Math.max(0, abs.precision() - abs.scale());
    }
}
