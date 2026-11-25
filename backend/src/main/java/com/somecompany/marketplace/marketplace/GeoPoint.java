package com.somecompany.marketplace.marketplace;

/** A latitude/longitude pair in decimal degrees. */
public record GeoPoint(double latitude, double longitude) {

    public static final GeoPoint DEFAULT_LOCATION = new GeoPoint(38.7893, -77.1873);
}
