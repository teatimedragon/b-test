package com.somecompany.marketplace.common;

import java.util.List;

/** Thrown when query parameters are invalid; mapped to 422. */
public class ParameterValidationException extends RuntimeException {

    private final List<FieldError> errors;

    public ParameterValidationException(List<FieldError> errors) {
        super("Invalid query parameters");
        this.errors = List.copyOf(errors);
    }

    public List<FieldError> getErrors() {
        return errors;
    }
}
