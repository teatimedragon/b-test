package com.somecompany.marketplace.validation;

import com.somecompany.marketplace.domain.ListingEntity;
import com.somecompany.marketplace.domain.ListingLogEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Maps uploaded values to the ListingLog and Listing tables. {@code id} goes to SourceId,
 * {@code source} to SourceCode and {@code sqft} to SquareFeet; everything else keeps its name.
 */
public class ListingMapper {

    public static final String CURRENCY = "USD";

    /** {@code source + ":" + id}, or null if either part is missing or blank. */
    public static String listingIdOf(String source, String id) {
        if (source == null || id == null || source.isBlank() || id.isBlank()) {
            return null;
        }
        return source.trim() + ":" + id.trim();
    }

    public String listingIdOf(RawListing raw) {
        return listingIdOf(raw.source(), raw.id());
    }

    /** The raw values exactly as received, truncated to each column's size. */
    public ListingLogEntity toLog(RawListing raw, UUID logId, Instant logTime) {
        ListingLogEntity log = new ListingLogEntity();
        log.setLogId(logId);
        log.setLogTime(logTime);
        log.setSourceId(fit(raw, ListingField.ID));
        log.setSourceCode(fit(raw, ListingField.SOURCE));
        log.setAddress(fit(raw, ListingField.ADDRESS));
        log.setCity(fit(raw, ListingField.CITY));
        log.setState(fit(raw, ListingField.STATE));
        log.setZip(fit(raw, ListingField.ZIP));
        log.setPrice(fit(raw, ListingField.PRICE));
        log.setCurrencyCode(CURRENCY);
        log.setBedrooms(fit(raw, ListingField.BEDROOMS));
        log.setBathrooms(fit(raw, ListingField.BATHROOMS));
        log.setSquareFeet(fit(raw, ListingField.SQFT));
        log.setLatitude(fit(raw, ListingField.LATITUDE));
        log.setLongitude(fit(raw, ListingField.LONGITUDE));
        log.setListedDate(fit(raw, ListingField.LISTED_DATE));
        log.setStatus(fit(raw, ListingField.STATUS));
        log.setDescription(fit(raw, ListingField.DESCRIPTION));
        return log;
    }

    /**
     * Converts a listing that passed validation. Text is trimmed, state is uppercased, status is
     * lowercased and latitude/longitude are rounded to 6 decimal places.
     */
    public ListingEntity toListing(RawListing raw) {
        ListingEntity listing = new ListingEntity();
        listing.setListingId(listingIdOf(raw));
        listing.setSourceId(raw.id().trim());
        listing.setSourceCode(raw.source().trim());
        listing.setAddress(raw.address().trim());
        listing.setCity(raw.city().trim());
        listing.setState(raw.state().trim().toUpperCase(Locale.ROOT));
        listing.setZip(raw.zip().trim());
        listing.setPrice(number(raw.price()).setScale(4, RoundingMode.UNNECESSARY));
        listing.setCurrencyCode(CURRENCY);
        listing.setBedrooms(number(raw.bedrooms()).shortValueExact());
        listing.setBathrooms(number(raw.bathrooms()).setScale(2, RoundingMode.UNNECESSARY));
        listing.setSquareFeet(number(raw.sqft()).intValueExact());
        listing.setLatitude(number(raw.latitude()).setScale(6, RoundingMode.HALF_UP));
        listing.setLongitude(number(raw.longitude()).setScale(6, RoundingMode.HALF_UP));
        listing.setListedDate(raw.listedDate().trim());
        listing.setStatus(raw.status().trim().toLowerCase(Locale.ROOT));
        listing.setDescription(raw.description().trim());
        return listing;
    }

    private static BigDecimal number(String value) {
        return Numbers.parse(value).orElseThrow(() -> new IllegalArgumentException("Not a number: " + value));
    }

    private static String fit(RawListing raw, ListingField field) {
        return Messages.truncate(field.valueOf(raw), field.maxLength());
    }
}
