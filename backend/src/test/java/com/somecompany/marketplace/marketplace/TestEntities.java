package com.somecompany.marketplace.marketplace;

import com.somecompany.marketplace.domain.ListingEntity;
import java.math.BigDecimal;

final class TestEntities {

    private TestEntities() {
    }

    static ListingEntity listing(String id, String status, String price, double lat, double lng, String listedDate) {
        ListingEntity l = new ListingEntity();
        l.setListingId("test:" + id);
        l.setSourceId(id);
        l.setSourceCode("test");
        l.setAddress(id + " Main St");
        l.setCity("Alexandria");
        l.setState("VA");
        l.setZip("22306");
        l.setPrice(new BigDecimal(price));
        l.setBedrooms((short) 3);
        l.setBathrooms(new BigDecimal("2.00"));
        l.setSquareFeet(1500);
        l.setLatitude(BigDecimal.valueOf(lat));
        l.setLongitude(BigDecimal.valueOf(lng));
        l.setListedDate(listedDate);
        l.setStatus(status);
        l.setDescription("Listing " + id);
        return l;
    }
}
