package com.somecompany.marketplace.marketplace;

import static com.somecompany.marketplace.marketplace.TestEntities.listing;
import static org.assertj.core.api.Assertions.assertThat;

import com.somecompany.marketplace.common.PageRequestParams;
import com.somecompany.marketplace.domain.ListingEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListingSearchFilterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
    private final ListingSearchFilter filter = new ListingSearchFilter();

    private static SearchCriteria criteria(Long min, Long max, Integer beds, String baths, String text) {
        return new SearchCriteria(min, max, beds, baths == null ? null : new BigDecimal(baths), text,
                GeoPoint.DEFAULT_LOCATION, new PageRequestParams(0, 20));
    }

    private List<String> run(List<ListingEntity> input, SearchCriteria c) {
        return filter.filter(input, c, TODAY).stream().map(ListingEntity::getSourceId).toList();
    }

    @Test
    void priceBoundsAreInclusive() {
        List<ListingEntity> input = List.of(
                listing("a", "active", "100", 0, 0, "2026-11-01"),
                listing("b", "active", "200", 0, 0, "2026-11-01"),
                listing("c", "active", "300", 0, 0, "2026-11-01"));
        assertThat(run(input, criteria(200L, null, null, null, null))).containsExactly("b", "c");
        assertThat(run(input, criteria(null, 200L, null, null, null))).containsExactly("a", "b");
        assertThat(run(input, criteria(200L, 200L, null, null, null))).containsExactly("b");
    }

    @Test
    void bedroomsAndBathroomsAreMinimums() {
        ListingEntity studio = listing("studio", "active", "100", 0, 0, "2026-11-01");
        studio.setBedrooms((short) 0);
        studio.setBathrooms(new BigDecimal("1.00"));
        ListingEntity big = listing("big", "active", "100", 0, 0, "2026-11-01");
        big.setBedrooms((short) 4);
        big.setBathrooms(new BigDecimal("1.50"));

        assertThat(run(List.of(studio, big), criteria(null, null, 1, null, null))).containsExactly("big");
        assertThat(run(List.of(studio, big), criteria(null, null, 0, null, null))).containsExactly("studio", "big");
        assertThat(run(List.of(studio, big), criteria(null, null, null, "1.5", null))).containsExactly("big");
    }

    @Test
    void freeTextMatchesCityStateZipOrDescriptionCaseInsensitively() {
        ListingEntity city = listing("city", "active", "100", 0, 0, "2026-11-01");
        city.setCity("Fairfax");
        ListingEntity state = listing("state", "active", "100", 0, 0, "2026-11-01");
        state.setState("MD");
        ListingEntity zip = listing("zip", "active", "100", 0, 0, "2026-11-01");
        zip.setZip("20001");
        ListingEntity desc = listing("desc", "active", "100", 0, 0, "2026-11-01");
        desc.setDescription("Sunny condo with a ROOFTOP pool");
        List<ListingEntity> input = List.of(city, state, zip, desc);

        assertThat(run(input, criteria(null, null, null, null, "FAIRfax"))).containsExactly("city");
        assertThat(run(input, criteria(null, null, null, null, "md"))).containsExactly("state");
        assertThat(run(input, criteria(null, null, null, null, "2000"))).containsExactly("zip");
        assertThat(run(input, criteria(null, null, null, null, "rooftop pool"))).containsExactly("desc");
    }

    @Test
    void freeTextIsOnePhraseNotSeparateWords() {
        ListingEntity desc = listing("desc", "active", "100", 0, 0, "2026-11-01");
        desc.setDescription("Sunny condo with a rooftop pool");
        assertThat(run(List.of(desc), criteria(null, null, null, null, "pool rooftop"))).isEmpty();
    }

    @Test
    void addressIsNotSearched() {
        ListingEntity l = listing("x", "active", "100", 0, 0, "2026-11-01");
        l.setAddress("1 Elm Street");
        assertThat(run(List.of(l), criteria(null, null, null, null, "Elm"))).isEmpty();
    }

    @Test
    void futureListingsAreExcludedButTodayIsKept() {
        List<ListingEntity> input = List.of(
                listing("today", "active", "100", 0, 0, "2026-11-25"),
                listing("tomorrow", "active", "100", 0, 0, "2026-11-26"));
        assertThat(run(input, criteria(null, null, null, null, null))).containsExactly("today");
    }

    @Test
    void filtersCombineWithAnd() {
        ListingEntity match = listing("match", "active", "250", 0, 0, "2026-11-01");
        ListingEntity cheap = listing("cheap", "active", "50", 0, 0, "2026-11-01");
        ListingEntity otherCity = listing("other", "active", "250", 0, 0, "2026-11-01");
        otherCity.setCity("Reston");
        assertThat(run(List.of(match, cheap, otherCity), criteria(100L, null, null, null, "alexandria")))
                .containsExactly("match");
    }
}
