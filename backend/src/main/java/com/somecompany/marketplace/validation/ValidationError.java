package com.somecompany.marketplace.validation;

/** One failed rule: the column that failed (or {@code general}) and a message with the failed value. */
public record ValidationError(String errorSource, String errorDetails) {

    public static final String GENERAL = "general";
    public static final int MAX_DETAILS_LENGTH = 100;

    public ValidationError {
        errorDetails = Messages.truncate(errorDetails, MAX_DETAILS_LENGTH);
    }

    public static ValidationError of(ListingField field, String details) {
        return new ValidationError(field.column(), details);
    }
}
