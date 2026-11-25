package com.somecompany.marketplace.common;

import java.util.List;

/** Validated 0-based page number and page size. */
public record PageRequestParams(int page, int size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    /**
     * Parses raw {@code page} and {@code size} query values, adding any problems to {@code errors}.
     * Returns defaults for values that are missing or invalid.
     */
    public static PageRequestParams parse(String page, String size, List<FieldError> errors) {
        int p = DEFAULT_PAGE;
        int s = DEFAULT_SIZE;
        if (page != null && !page.isBlank()) {
            Integer parsed = parseInt(page);
            if (parsed == null) {
                errors.add(new FieldError("page", "Validation Failure, page (" + page + ") is not a whole number"));
            } else if (parsed < 0) {
                errors.add(new FieldError("page", "Validation Failure, page (" + page + ") must be 0 or greater"));
            } else {
                p = parsed;
            }
        }
        if (size != null && !size.isBlank()) {
            Integer parsed = parseInt(size);
            if (parsed == null) {
                errors.add(new FieldError("size", "Validation Failure, size (" + size + ") is not a whole number"));
            } else if (parsed < 1 || parsed > MAX_SIZE) {
                errors.add(new FieldError("size",
                        "Validation Failure, size (" + size + ") must be between 1 and " + MAX_SIZE));
            } else {
                s = parsed;
            }
        }
        return new PageRequestParams(p, s);
    }

    /** Parses and throws a 422 if invalid. */
    public static PageRequestParams parseOrThrow(String page, String size) {
        List<FieldError> errors = new java.util.ArrayList<>();
        PageRequestParams params = parse(page, size, errors);
        if (!errors.isEmpty()) {
            throw new ParameterValidationException(errors);
        }
        return params;
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public long offset() {
        return (long) page * size;
    }

    /** Returns the slice of an already ordered list for this page (empty past the end). */
    public <T> List<T> slice(List<T> all) {
        long from = offset();
        if (from >= all.size()) {
            return List.of();
        }
        int to = (int) Math.min(all.size(), from + size);
        return all.subList((int) from, to);
    }
}
