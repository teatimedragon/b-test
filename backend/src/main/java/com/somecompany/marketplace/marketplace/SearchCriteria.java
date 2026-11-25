package com.somecompany.marketplace.marketplace;

import com.somecompany.marketplace.common.PageRequestParams;
import java.math.BigDecimal;

/** Validated search parameters. Null bounds mean "not filtered"; freeText is trimmed or null. */
public record SearchCriteria(
        Long minPrice,
        Long maxPrice,
        Integer minBedrooms,
        BigDecimal minBathrooms,
        String freeText,
        GeoPoint location,
        PageRequestParams page) {
}
