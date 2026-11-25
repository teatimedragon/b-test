package com.somecompany.marketplace.validation;

/** Helpers for building error messages that fit the 100-character ErrorDetails column. */
public final class Messages {

    /** Longest raw value quoted in a message, so the reason still fits in ErrorDetails. */
    static final int MAX_QUOTED_VALUE = 30;

    private Messages() {
    }

    /** {@code 'value'}, shortened with "..." when long. */
    public static String quote(String value) {
        if (value == null) {
            return "'null'";
        }
        String v = value.length() > MAX_QUOTED_VALUE ? value.substring(0, MAX_QUOTED_VALUE - 3) + "..." : value;
        return "'" + v + "'";
    }

    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
