package com.somecompany.marketplace.marketplace;

/** Great-circle distance between two points, in miles. */
public final class Haversine {

    /** Mean Earth radius in miles. */
    public static final double EARTH_RADIUS_MILES = 3958.8;

    private Haversine() {
    }

    public static double miles(GeoPoint a, GeoPoint b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_MILES * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }
}
