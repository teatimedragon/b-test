package com.somecompany.marketplace.common;

/** One query parameter validation problem, reported in a 422 body. */
public record FieldError(String field, String message) {
}
