package com.somecompany.marketplace.common;

import java.math.BigDecimal;

/** Output formatting for stored decimals (e.g. price 450000.0000 is returned as 450000.00). */
public final class Decimals {

    private Decimals() {
    }

    /** Price with at least 2 decimal places, dropping extra trailing zeros. */
    public static BigDecimal price(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 2 ? stripped.setScale(2) : stripped;
    }

    /** Without trailing zeros (2.50 becomes 2.5, 2.00 becomes 2). */
    public static BigDecimal compact(BigDecimal value) {
        if (value == null) {
            return null;
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
