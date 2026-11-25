package com.somecompany.marketplace.marketplace;

import com.somecompany.marketplace.domain.ListingEntity;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Orders a filtered result set: grouped by status (active, pending, sold), then by weighted score
 * (highest first), then newer listedDate, then listingId ascending.
 *
 * <pre>
 * score          = 0.4 * distanceScore + 0.4 * priceScore + 0.2 * freshnessScore
 * distanceScore  = 10 - 9 * (distance / dMax)                 (10 for all when dMax == 0)
 * priceScore     = 10 - 9 * ((price - pMin) / (pMax - pMin))  (10 for all when pMax == pMin)
 * freshnessScore = max(1, 10 - floor(ageDays / 5))
 * </pre>
 *
 * dMax, pMin and pMax come from the whole input (all statuses), so scores compare across groups.
 */
public class ListingRanker {

    public static final double DISTANCE_WEIGHT = 0.4;
    public static final double PRICE_WEIGHT = 0.4;
    public static final double FRESHNESS_WEIGHT = 0.2;

    private static final Map<String, Integer> STATUS_ORDER = Map.of("active", 0, "pending", 1, "sold", 2);

    public List<RankedListing> rank(List<ListingEntity> listings, GeoPoint origin, LocalDate today) {
        if (listings.isEmpty()) {
            return List.of();
        }
        double[] distances = listings.stream()
                .mapToDouble(l -> Haversine.miles(origin, pointOf(l)))
                .toArray();
        double dMax = 0;
        for (double d : distances) {
            dMax = Math.max(dMax, d);
        }
        double pMin = listings.stream().mapToDouble(l -> l.getPrice().doubleValue()).min().orElse(0);
        double pMax = listings.stream().mapToDouble(l -> l.getPrice().doubleValue()).max().orElse(0);

        java.util.ArrayList<RankedListing> ranked = new java.util.ArrayList<>(listings.size());
        for (int i = 0; i < listings.size(); i++) {
            ListingEntity l = listings.get(i);
            double ds = distanceScore(distances[i], dMax);
            double ps = priceScore(l.getPrice().doubleValue(), pMin, pMax);
            double fs = freshnessScore(ageDays(l, today));
            ranked.add(new RankedListing(l, distances[i], ds, ps, fs, weightedScore(ds, ps, fs)));
        }
        ranked.sort(ORDER);
        return List.copyOf(ranked);
    }

    public static double distanceScore(double distance, double dMax) {
        if (dMax <= 0) {
            return 10;
        }
        return 10 - 9 * (distance / dMax);
    }

    public static double priceScore(double price, double pMin, double pMax) {
        if (pMax == pMin) {
            return 10;
        }
        return 10 - 9 * ((price - pMin) / (pMax - pMin));
    }

    public static double freshnessScore(long ageDays) {
        long age = Math.max(0, ageDays);
        return Math.max(1, 10 - Math.floorDiv(age, 5));
    }

    public static double weightedScore(double distanceScore, double priceScore, double freshnessScore) {
        return DISTANCE_WEIGHT * distanceScore + PRICE_WEIGHT * priceScore + FRESHNESS_WEIGHT * freshnessScore;
    }

    static long ageDays(ListingEntity l, LocalDate today) {
        return ChronoUnit.DAYS.between(LocalDate.parse(l.getListedDate()), today);
    }

    private static GeoPoint pointOf(ListingEntity l) {
        return new GeoPoint(l.getLatitude().doubleValue(), l.getLongitude().doubleValue());
    }

    static int statusRank(String status) {
        return STATUS_ORDER.getOrDefault(status, STATUS_ORDER.size());
    }

    static final Comparator<RankedListing> ORDER = Comparator
            .comparingInt((RankedListing r) -> statusRank(r.listing().getStatus()))
            .thenComparing(RankedListing::score, Comparator.reverseOrder())
            .thenComparing((RankedListing r) -> r.listing().getListedDate(), Comparator.reverseOrder())
            .thenComparing(r -> r.listing().getListingId());
}
