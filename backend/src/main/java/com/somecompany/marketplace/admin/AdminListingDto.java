package com.somecompany.marketplace.admin;

import com.somecompany.marketplace.common.Decimals;
import com.somecompany.marketplace.domain.ListingEntity;
import java.math.BigDecimal;

/** A Listing row in the admin API, using the upload field names ({@code id}, {@code source}, {@code sqft}). */
public record AdminListingDto(
        String listingId,
        String id,
        String source,
        String address,
        String city,
        String state,
        String zip,
        BigDecimal price,
        String currencyCode,
        Integer bedrooms,
        BigDecimal bathrooms,
        Integer sqft,
        BigDecimal latitude,
        BigDecimal longitude,
        String listedDate,
        String status,
        String description) {

    public static AdminListingDto from(ListingEntity l) {
        return new AdminListingDto(l.getListingId(), l.getSourceId(), l.getSourceCode(), l.getAddress(),
                l.getCity(), l.getState(), l.getZip(), Decimals.price(l.getPrice()), l.getCurrencyCode(),
                l.getBedrooms() == null ? null : l.getBedrooms().intValue(), Decimals.compact(l.getBathrooms()),
                l.getSquareFeet(), l.getLatitude(), l.getLongitude(), l.getListedDate(), l.getStatus(),
                l.getDescription());
    }
}
