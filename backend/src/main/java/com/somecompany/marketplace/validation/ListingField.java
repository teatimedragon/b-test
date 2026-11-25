package com.somecompany.marketplace.validation;

import java.util.function.Function;

/**
 * The uploaded fields: request name, column name (used as ErrorSource), the size of the column
 * (ListingLog and Listing text columns share the same sizes) and how to read the raw value.
 */
public enum ListingField {
    ID("id", "SourceId", 50, RawListing::id),
    SOURCE("source", "SourceCode", 100, RawListing::source),
    ADDRESS("address", "Address", 100, RawListing::address),
    CITY("city", "City", 100, RawListing::city),
    STATE("state", "State", 2, RawListing::state),
    ZIP("zip", "Zip", 50, RawListing::zip),
    PRICE("price", "Price", 100, RawListing::price),
    BEDROOMS("bedrooms", "Bedrooms", 50, RawListing::bedrooms),
    BATHROOMS("bathrooms", "Bathrooms", 50, RawListing::bathrooms),
    SQFT("sqft", "SquareFeet", 100, RawListing::sqft),
    LATITUDE("latitude", "Latitude", 20, RawListing::latitude),
    LONGITUDE("longitude", "Longitude", 20, RawListing::longitude),
    LISTED_DATE("listedDate", "ListedDate", 100, RawListing::listedDate),
    STATUS("status", "Status", 50, RawListing::status),
    DESCRIPTION("description", "Description", 250, RawListing::description);

    private final String requestName;
    private final String column;
    private final int maxLength;
    private final Function<RawListing, String> getter;

    ListingField(String requestName, String column, int maxLength, Function<RawListing, String> getter) {
        this.requestName = requestName;
        this.column = column;
        this.maxLength = maxLength;
        this.getter = getter;
    }

    public String requestName() { return requestName; }
    public String column() { return column; }
    public int maxLength() { return maxLength; }

    public String valueOf(RawListing listing) {
        return getter.apply(listing);
    }
}
