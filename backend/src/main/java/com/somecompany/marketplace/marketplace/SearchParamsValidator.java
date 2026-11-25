package com.somecompany.marketplace.marketplace;

import com.somecompany.marketplace.common.FieldError;
import com.somecompany.marketplace.common.PageRequestParams;
import com.somecompany.marketplace.common.ParameterValidationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses and validates the raw {@code GET /marketplace/listing} query values. All problems are
 * collected and reported together as a {@link ParameterValidationException} (422).
 */
public class SearchParamsValidator {

    /** The raw query values as received (any may be null). */
    public record RawParams(String minPrice, String maxPrice, String minBedrooms, String minBathrooms,
            String freeText, String lat, String lng, String page, String size) {
    }

    public SearchCriteria validate(RawParams raw) {
        List<FieldError> errors = new ArrayList<>();

        Long minPrice = wholeAtLeastZero("min_price", "min price", raw.minPrice(), errors);
        Long maxPrice = wholeAtLeastZero("max_price", "max price", raw.maxPrice(), errors);
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            errors.add(new FieldError("min_price", "Validation Failure, min price (" + minPrice
                    + ") is greater than max price (" + maxPrice + ")"));
        }
        Long minBedrooms = wholeAtLeastZero("min_bedrooms", "min bedrooms", raw.minBedrooms(), errors);
        if (minBedrooms != null && minBedrooms > Short.MAX_VALUE) {
            errors.add(new FieldError("min_bedrooms",
                    "Validation Failure, min bedrooms (" + minBedrooms + ") must be at most " + Short.MAX_VALUE));
            minBedrooms = null;
        }
        BigDecimal minBathrooms = decimal("min_bathrooms", "min bathrooms", raw.minBathrooms(), errors);
        if (minBathrooms != null && minBathrooms.signum() < 0) {
            errors.add(new FieldError("min_bathrooms",
                    "Validation Failure, min bathrooms (" + raw.minBathrooms().trim() + ") must be 0 or greater"));
        }

        GeoPoint location = location(raw.lat(), raw.lng(), errors);
        PageRequestParams page = PageRequestParams.parse(raw.page(), raw.size(), errors);

        if (!errors.isEmpty()) {
            throw new ParameterValidationException(errors);
        }
        String freeText = raw.freeText() == null || raw.freeText().isBlank() ? null : raw.freeText().trim();
        return new SearchCriteria(minPrice, maxPrice, minBedrooms == null ? null : minBedrooms.intValue(),
                minBathrooms, freeText, location, page);
    }

    private static GeoPoint location(String lat, String lng, List<FieldError> errors) {
        boolean hasLat = lat != null && !lat.isBlank();
        boolean hasLng = lng != null && !lng.isBlank();
        if (!hasLat && !hasLng) {
            return GeoPoint.DEFAULT_LOCATION;
        }
        if (hasLat != hasLng) {
            String missing = hasLat ? "lng" : "lat";
            errors.add(new FieldError(missing,
                    "Validation Failure, lat and lng must be sent together (" + missing + " is missing)"));
            return null;
        }
        BigDecimal latValue = decimal("lat", "lat", lat, errors);
        BigDecimal lngValue = decimal("lng", "lng", lng, errors);
        if (latValue != null && latValue.abs().compareTo(BigDecimal.valueOf(90)) > 0) {
            errors.add(new FieldError("lat", "Validation Failure, lat (" + lat.trim() + ") must be between -90 and 90"));
        }
        if (lngValue != null && lngValue.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            errors.add(new FieldError("lng",
                    "Validation Failure, lng (" + lng.trim() + ") must be between -180 and 180"));
        }
        if (latValue == null || lngValue == null) {
            return null;
        }
        return new GeoPoint(latValue.doubleValue(), lngValue.doubleValue());
    }

    private static Long wholeAtLeastZero(String field, String label, String raw, List<FieldError> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        long parsed;
        try {
            parsed = Long.parseLong(value);
        } catch (NumberFormatException e) {
            errors.add(new FieldError(field, "Validation Failure, " + label + " (" + value + ") is not a whole number"));
            return null;
        }
        if (parsed < 0) {
            errors.add(new FieldError(field, "Validation Failure, " + label + " (" + value + ") must be 0 or greater"));
            return null;
        }
        return parsed;
    }

    private static BigDecimal decimal(String field, String label, String raw, List<FieldError> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            errors.add(new FieldError(field, "Validation Failure, " + label + " (" + value + ") is not a number"));
            return null;
        }
    }
}
