package com.somecompany.marketplace.marketplace;

import static com.somecompany.marketplace.marketplace.TestEntities.listing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.somecompany.marketplace.domain.ListingEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ListingRankerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private static final GeoPoint ORIGIN = new GeoPoint(38.7893, -77.1873);

    private final ListingRanker ranker = new ListingRanker();

    private List<String> ids(List<RankedListing> ranked) {
        return ranked.stream().map(r -> r.listing().getSourceId()).toList();
    }

    @Test
    void emptyInputGivesEmptyOutput() {
        assertThat(ranker.rank(List.of(), ORIGIN, TODAY)).isEmpty();
    }

    @Test
    void groupsActiveThenPendingThenSoldRegardlessOfScore() {
        // The sold listing is the best on every measure but must still come last.
        List<ListingEntity> input = List.of(
                listing("sold", "sold", "100000", 38.7893, -77.1873, "2026-11-25"),
                listing("pending", "pending", "200000", 38.80, -77.20, "2026-11-20"),
                listing("active", "active", "900000", 39.50, -78.00, "2026-09-01"));

        assertThat(ids(ranker.rank(input, ORIGIN, TODAY))).containsExactly("active", "pending", "sold");
    }

    @Test
    void ranksWithinAGroupByScoreHighestFirst() {
        List<ListingEntity> input = List.of(
                listing("far-expensive", "active", "900000", 39.50, -78.00, "2026-11-25"),
                listing("near-cheap", "active", "100000", 38.79, -77.19, "2026-11-25"),
                listing("middle", "active", "500000", 39.10, -77.60, "2026-11-25"));

        List<RankedListing> ranked = ranker.rank(input, ORIGIN, TODAY);
        assertThat(ids(ranked)).containsExactly("near-cheap", "middle", "far-expensive");
        assertThat(ranked.get(0).score()).isGreaterThan(ranked.get(1).score());
    }

    @Test
    void minAndMaxComeFromTheWholeSetAcrossStatuses() {
        // Only the sold listing is cheap; the active one is the most expensive in the whole set,
        // so its price score is 1, not 10 as it would be if scaled within its own group.
        List<ListingEntity> input = List.of(
                listing("active", "active", "500000", 38.7893, -77.1873, "2026-11-25"),
                listing("sold", "sold", "100000", 38.7893, -77.1873, "2026-11-25"));

        RankedListing active = ranker.rank(input, ORIGIN, TODAY).get(0);
        assertThat(active.priceScore()).isEqualTo(1.0);
    }

    @Nested
    class Scores {

        @Test
        void distanceScoreIsTenAtZeroAndOneAtTheFurthest() {
            assertThat(ListingRanker.distanceScore(0, 20)).isEqualTo(10);
            assertThat(ListingRanker.distanceScore(20, 20)).isEqualTo(1);
            assertThat(ListingRanker.distanceScore(10, 20)).isEqualTo(5.5);
        }

        @Test
        void distanceScoreIsTenWhenAllDistancesAreZero() {
            assertThat(ListingRanker.distanceScore(0, 0)).isEqualTo(10);
        }

        @Test
        void priceScoreIsTenForCheapestAndOneForMostExpensive() {
            assertThat(ListingRanker.priceScore(100, 100, 300)).isEqualTo(10);
            assertThat(ListingRanker.priceScore(300, 100, 300)).isEqualTo(1);
            assertThat(ListingRanker.priceScore(200, 100, 300)).isEqualTo(5.5);
        }

        @Test
        void priceScoreIsTenWhenAllPricesAreEqual() {
            assertThat(ListingRanker.priceScore(250, 250, 250)).isEqualTo(10);
        }

        @ParameterizedTest
        @CsvSource({"0,10", "4,10", "5,9", "9,9", "10,8", "14,8", "40,2", "44,2", "45,1", "100,1", "-3,10"})
        void freshnessScoreTable(long ageDays, double expected) {
            assertThat(ListingRanker.freshnessScore(ageDays)).isEqualTo(expected);
        }

        @Test
        void weightedScoreUses40_40_20() {
            assertThat(ListingRanker.weightedScore(10, 10, 10)).isCloseTo(10, within(1e-9));
            assertThat(ListingRanker.weightedScore(10, 0, 0)).isCloseTo(4, within(1e-9));
            assertThat(ListingRanker.weightedScore(0, 10, 0)).isCloseTo(4, within(1e-9));
            assertThat(ListingRanker.weightedScore(0, 0, 10)).isCloseTo(2, within(1e-9));
            assertThat(ListingRanker.weightedScore(5.5, 1, 9)).isCloseTo(0.4 * 5.5 + 0.4 * 1 + 0.2 * 9, within(1e-9));
        }

        @Test
        void singleResultIsItsOwnFurthestAndCheapest() {
            // A lone listing away from the origin is also the furthest (dMax), so the formula gives it 1;
            // pMax == pMin, so its price score is 10.
            RankedListing only = ranker.rank(
                    List.of(listing("a", "active", "300000", 39.0, -77.5, "2026-11-25")), ORIGIN, TODAY).get(0);
            assertThat(only.distanceScore()).isEqualTo(1);
            assertThat(only.priceScore()).isEqualTo(10);
            assertThat(only.freshnessScore()).isEqualTo(10);
            assertThat(only.score()).isCloseTo(0.4 * 1 + 0.4 * 10 + 0.2 * 10, within(1e-9));
        }

        @Test
        void singleResultAtTheOriginScoresTen() {
            RankedListing only = ranker.rank(
                    List.of(listing("a", "active", "300000", 38.7893, -77.1873, "2026-11-25")), ORIGIN, TODAY)
                    .get(0);
            assertThat(only.distanceScore()).isEqualTo(10);
            assertThat(only.score()).isCloseTo(10, within(1e-9));
        }

        @Test
        void listingAtTheOriginHasZeroDistance() {
            List<RankedListing> ranked = ranker.rank(List.of(
                    listing("here", "active", "300000", 38.7893, -77.1873, "2026-11-25"),
                    listing("there", "active", "300000", 39.7893, -77.1873, "2026-11-25")), ORIGIN, TODAY);
            assertThat(ranked.get(0).distanceMiles()).isZero();
            assertThat(ranked.get(0).distanceScore()).isEqualTo(10);
            assertThat(ranked.get(1).distanceScore()).isEqualTo(1);
        }

        @Test
        void computesEachPartForAKnownSet() {
            // 3 listings on the same meridian: 0, 1 and 2 degrees north of the origin.
            List<RankedListing> ranked = ranker.rank(List.of(
                    listing("a", "active", "100000", 38.7893, -77.1873, "2026-11-25"),  // age 0
                    listing("b", "active", "200000", 39.7893, -77.1873, "2026-11-18"),  // age 7
                    listing("c", "active", "300000", 40.7893, -77.1873, "2026-10-01")), // age 55
                    ORIGIN, TODAY);

            RankedListing b = ranked.stream().filter(r -> r.listing().getSourceId().equals("b")).findFirst().get();
            assertThat(b.distanceScore()).isCloseTo(5.5, within(1e-6));
            assertThat(b.priceScore()).isCloseTo(5.5, within(1e-9));
            assertThat(b.freshnessScore()).isEqualTo(9);
            assertThat(b.ageDays()).isEqualTo(7);
            assertThat(b.score()).isCloseTo(0.4 * 5.5 + 0.4 * 5.5 + 0.2 * 9, within(1e-6));
            assertThat(ids(ranked)).containsExactly("a", "b", "c");
        }
    }

    @Nested
    class TieBreakers {

        @Test
        void equalScoresPutTheNewerListedDateFirst() {
            // Same place and price; ages 0 and 3 both have freshness 10, so the scores tie.
            List<ListingEntity> input = List.of(
                    listing("older", "active", "300000", 38.9, -77.3, "2026-11-22"),
                    listing("newer", "active", "300000", 38.9, -77.3, "2026-11-25"));

            List<RankedListing> ranked = ranker.rank(input, ORIGIN, TODAY);
            assertThat(ranked.get(0).score()).isEqualTo(ranked.get(1).score());
            assertThat(ids(ranked)).containsExactly("newer", "older");
        }

        @Test
        void thenListingIdAscending() {
            List<ListingEntity> input = List.of(
                    listing("c", "active", "300000", 38.9, -77.3, "2026-11-25"),
                    listing("a", "active", "300000", 38.9, -77.3, "2026-11-25"),
                    listing("b", "active", "300000", 38.9, -77.3, "2026-11-25"));

            assertThat(ids(ranker.rank(input, ORIGIN, TODAY))).containsExactly("a", "b", "c");
        }
    }

    @Test
    void originChangesTheOrder() {
        List<ListingEntity> input = List.of(
                listing("north", "active", "300000", 40.0, -77.0, "2026-11-25"),
                listing("south", "active", "300000", 37.0, -77.0, "2026-11-25"));

        assertThat(ids(ranker.rank(input, new GeoPoint(40.0, -77.0), TODAY))).containsExactly("north", "south");
        assertThat(ids(ranker.rank(input, new GeoPoint(37.0, -77.0), TODAY))).containsExactly("south", "north");
    }
}
