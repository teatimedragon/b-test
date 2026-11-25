package com.somecompany.marketplace.validation;

import java.util.List;
import java.util.function.Function;

/** A small, independent validation rule. Returns an empty list when the listing passes. */
@FunctionalInterface
public interface ListingValidator extends Function<RawListing, List<ValidationError>> {
}
