package com.somecompany.marketplace.common;

/** The {@code meta} object returned with every paginated list. */
public record PageMeta(int page, int size, int totalPages, long totalElements) {

    public static PageMeta of(int page, int size, long totalElements) {
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageMeta(page, size, totalPages, totalElements);
    }
}
