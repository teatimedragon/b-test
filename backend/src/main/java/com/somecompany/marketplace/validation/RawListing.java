package com.somecompany.marketplace.validation;

/**
 * One uploaded listing with every value converted to its string form (or {@code null} when the
 * field was missing or JSON null). {@code jsonObject} is false when the array entry was not a
 * JSON object at all; {@code rawJson} keeps that entry's text for the error message.
 */
public record RawListing(
        String id,
        String source,
        String address,
        String city,
        String state,
        String zip,
        String price,
        String bedrooms,
        String bathrooms,
        String sqft,
        String latitude,
        String longitude,
        String listedDate,
        String status,
        String description,
        boolean jsonObject,
        String rawJson) {

    /** An entry that was not a JSON object: every field empty. */
    public static RawListing notAnObject(String rawJson) {
        return new RawListing(null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, false, rawJson);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Convenient for tests and the JSON reader. */
    public static final class Builder {
        private String id;
        private String source;
        private String address;
        private String city;
        private String state;
        private String zip;
        private String price;
        private String bedrooms;
        private String bathrooms;
        private String sqft;
        private String latitude;
        private String longitude;
        private String listedDate;
        private String status;
        private String description;

        public Builder id(String v) { this.id = v; return this; }
        public Builder source(String v) { this.source = v; return this; }
        public Builder address(String v) { this.address = v; return this; }
        public Builder city(String v) { this.city = v; return this; }
        public Builder state(String v) { this.state = v; return this; }
        public Builder zip(String v) { this.zip = v; return this; }
        public Builder price(String v) { this.price = v; return this; }
        public Builder bedrooms(String v) { this.bedrooms = v; return this; }
        public Builder bathrooms(String v) { this.bathrooms = v; return this; }
        public Builder sqft(String v) { this.sqft = v; return this; }
        public Builder latitude(String v) { this.latitude = v; return this; }
        public Builder longitude(String v) { this.longitude = v; return this; }
        public Builder listedDate(String v) { this.listedDate = v; return this; }
        public Builder status(String v) { this.status = v; return this; }
        public Builder description(String v) { this.description = v; return this; }

        public RawListing build() {
            return new RawListing(id, source, address, city, state, zip, price, bedrooms, bathrooms, sqft,
                    latitude, longitude, listedDate, status, description, true, null);
        }
    }
}
