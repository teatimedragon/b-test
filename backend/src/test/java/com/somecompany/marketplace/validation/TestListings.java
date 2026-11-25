package com.somecompany.marketplace.validation;

/** A listing that passes every rule; tests change one field at a time. */
public final class TestListings {

    private TestListings() {
    }

    public static RawListing.Builder valid() {
        return RawListing.builder()
                .id("12345")
                .source("zillow")
                .address("123 Main St")
                .city("Alexandria")
                .state("VA")
                .zip("22306")
                .price("450000")
                .bedrooms("3")
                .bathrooms("2.5")
                .sqft("1850")
                .latitude("38.7893")
                .longitude("-77.1873")
                .listedDate("2026-11-20")
                .status("active")
                .description("Bright colonial near the park.");
    }
}
