package com.somecompany.marketplace.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class HaversineTest {

    @Test
    void samePointIsZero() {
        GeoPoint p = new GeoPoint(38.7893, -77.1873);
        assertThat(Haversine.miles(p, p)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAbout69Miles() {
        assertThat(Haversine.miles(new GeoPoint(0, 0), new GeoPoint(1, 0))).isCloseTo(69.09, within(0.05));
    }

    @Test
    void newYorkToLosAngeles() {
        GeoPoint nyc = new GeoPoint(40.7128, -74.0060);
        GeoPoint la = new GeoPoint(34.0522, -118.2437);
        assertThat(Haversine.miles(nyc, la)).isCloseTo(2445.6, within(2.0));
    }

    @Test
    void washingtonToBaltimore() {
        GeoPoint dc = new GeoPoint(38.9072, -77.0369);
        GeoPoint baltimore = new GeoPoint(39.2904, -76.6122);
        assertThat(Haversine.miles(dc, baltimore)).isCloseTo(35.3, within(0.5));
    }

    @Test
    void isSymmetric() {
        GeoPoint a = new GeoPoint(51.5074, -0.1278);
        GeoPoint b = new GeoPoint(48.8566, 2.3522);
        assertThat(Haversine.miles(a, b)).isCloseTo(Haversine.miles(b, a), within(1e-9));
        assertThat(Haversine.miles(a, b)).isCloseTo(213.5, within(1.0));
    }

    @Test
    void antipodesAreHalfTheCircumference() {
        assertThat(Haversine.miles(new GeoPoint(0, 0), new GeoPoint(0, 180)))
                .isCloseTo(Math.PI * Haversine.EARTH_RADIUS_MILES, within(0.01));
    }
}
