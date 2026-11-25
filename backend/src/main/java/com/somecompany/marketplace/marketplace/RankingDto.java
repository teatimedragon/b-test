package com.somecompany.marketplace.marketplace;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Why a listing is where it is in the results, so the ranking can be checked by hand:
 * {@code score = 0.4 * distanceScore + 0.4 * priceScore + 0.2 * freshnessScore}.
 * Scores and distance are rounded to 4 decimal places for display; ordering uses the unrounded values.
 */
public record RankingDto(
        BigDecimal score,
        BigDecimal distanceScore,
        BigDecimal priceScore,
        BigDecimal freshnessScore,
        BigDecimal distanceMiles,
        long ageDays) {

    static final int SCALE = 4;

    public static RankingDto from(RankedListing r) {
        return new RankingDto(round(r.score()), round(r.distanceScore()), round(r.priceScore()),
                round(r.freshnessScore()), round(r.distanceMiles()), r.ageDays());
    }

    static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
