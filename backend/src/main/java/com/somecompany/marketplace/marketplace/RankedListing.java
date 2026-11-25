package com.somecompany.marketplace.marketplace;

import com.somecompany.marketplace.domain.ListingEntity;

/** A listing with the parts of its ranking score (kept for tests and debugging). */
public record RankedListing(
        ListingEntity listing,
        double distanceMiles,
        long ageDays,
        double distanceScore,
        double priceScore,
        double freshnessScore,
        double score) {
}
