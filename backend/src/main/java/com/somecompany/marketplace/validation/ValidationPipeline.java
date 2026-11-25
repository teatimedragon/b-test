package com.somecompany.marketplace.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a listing through every validator in order and collects all errors (it never stops at the
 * first failure).
 */
public class ValidationPipeline {

    private final List<ListingValidator> validators;

    public ValidationPipeline(List<ListingValidator> validators) {
        this.validators = List.copyOf(validators);
    }

    /** The full rule set from the spec, in a stable order. */
    public static ValidationPipeline standard() {
        List<ListingValidator> validators = new ArrayList<>();
        for (ListingField field : ListingField.values()) {
            validators.add(ListingValidators.required(field));
            // State's own rule ("exactly 2 letters") already covers its length.
            if (field != ListingField.STATE) {
                validators.add(ListingValidators.maxLength(field));
            }
        }
        validators.add(ListingValidators.listingIdLength());
        validators.add(ListingValidators.state());
        validators.add(ListingValidators.status());
        validators.add(ListingValidators.listedDate());
        validators.add(ListingValidators.price());
        validators.add(ListingValidators.bedrooms());
        validators.add(ListingValidators.bathrooms());
        validators.add(ListingValidators.sqft());
        validators.add(ListingValidators.latitude());
        validators.add(ListingValidators.longitude());
        return new ValidationPipeline(validators);
    }

    public List<ValidationError> validate(RawListing listing) {
        if (!listing.jsonObject()) {
            return List.of(new ValidationError(ValidationError.GENERAL,
                    "Listing " + Messages.quote(listing.rawJson()) + " is not a JSON object"));
        }
        return validators.stream()
                .flatMap(validator -> validator.apply(listing).stream())
                .toList();
    }

    public int size() {
        return validators.size();
    }
}
