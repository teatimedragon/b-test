package com.somecompany.marketplace.marketplace;

import static java.util.Map.entry;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.somecompany.marketplace.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class MarketplaceApiIntegrationTest extends IntegrationTestSupport {

    private ResultActions search(String... params) throws Exception {
        var request = get("/marketplace/listing");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mvc.perform(request);
    }

    private static Map<String, Object> at(String id, Object price, double lat, double lng, String date,
            String status) {
        return with(listing(id), entry("price", price), entry("latitude", lat), entry("longitude", lng),
                entry("listedDate", date), entry("status", status));
    }

    @Test
    void returnsListingsWithMarketplaceFieldNamesAndMeta() throws Exception {
        upload(List.of(listing("12345")));

        search()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings[0].listingId").value("zillow:12345"))
                .andExpect(jsonPath("$.listings[0].sourceId").value("12345"))
                .andExpect(jsonPath("$.listings[0].sourceCode").value("zillow"))
                .andExpect(jsonPath("$.listings[0].address").value("123 Main St"))
                .andExpect(jsonPath("$.listings[0].city").value("Alexandria"))
                .andExpect(jsonPath("$.listings[0].state").value("VA"))
                .andExpect(jsonPath("$.listings[0].zip").value("22306"))
                .andExpect(jsonPath("$.listings[0].price").value(450000.00))
                .andExpect(jsonPath("$.listings[0].currencyCode").value("USD"))
                .andExpect(jsonPath("$.listings[0].bedrooms").value(3))
                .andExpect(jsonPath("$.listings[0].bathrooms").value(2.5))
                .andExpect(jsonPath("$.listings[0].sqft").value(1850))
                .andExpect(jsonPath("$.listings[0].latitude").value(38.7893))
                .andExpect(jsonPath("$.listings[0].longitude").value(-77.1873))
                .andExpect(jsonPath("$.listings[0].listedDate").value("2026-11-20"))
                .andExpect(jsonPath("$.listings[0].status").value("active"))
                .andExpect(jsonPath("$.listings[0].description").value("Bright colonial near the park."))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(20))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void emptyDatabaseReturnsEmptyList() throws Exception {
        search().andExpect(status().isOk())
                .andExpect(jsonPath("$.listings", hasSize(0)))
                .andExpect(jsonPath("$.meta.totalElements").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(0));
    }

    @Test
    void priceFilterIsInclusive() throws Exception {
        upload(List.of(with(listing("a"), entry("price", 100000)), with(listing("b"), entry("price", 200000)),
                with(listing("c"), entry("price", 300000))));

        search("min_price", "200000").andExpect(jsonPath("$.listings[*].sourceId", containsInAnyOrder("b", "c")));
        search("max_price", "200000").andExpect(jsonPath("$.listings[*].sourceId", containsInAnyOrder("a", "b")));
        search("min_price", "200000", "max_price", "200000").andExpect(jsonPath("$.listings[*].sourceId",
                contains("b")));
    }

    @Test
    void bedroomAndBathroomFilters() throws Exception {
        upload(List.of(
                with(listing("studio"), entry("bedrooms", 0), entry("bathrooms", 1)),
                with(listing("two"), entry("bedrooms", 2), entry("bathrooms", 1.5)),
                with(listing("four"), entry("bedrooms", 4), entry("bathrooms", 3))));

        search("min_bedrooms", "2").andExpect(jsonPath("$.listings[*].sourceId", containsInAnyOrder("two", "four")));
        search("min_bathrooms", "1.5").andExpect(jsonPath("$.listings[*].sourceId",
                containsInAnyOrder("two", "four")));
        search("min_bathrooms", "2").andExpect(jsonPath("$.listings[*].sourceId", contains("four")));
        search("min_bedrooms", "0").andExpect(jsonPath("$.listings", hasSize(3)));
    }

    @Test
    void freeTextIsCaseInsensitiveAcrossCityStateZipAndDescription() throws Exception {
        upload(List.of(
                with(listing("city"), entry("city", "Fairfax"), entry("description", "x")),
                with(listing("state"), entry("state", "MD"), entry("city", "Bethesda"), entry("description", "x")),
                with(listing("zip"), entry("zip", "20001"), entry("city", "Washington"), entry("state", "DC"),
                        entry("description", "x")),
                with(listing("desc"), entry("description", "Sunny condo with a Rooftop Pool"))));

        search("free_text", "fairFAX").andExpect(jsonPath("$.listings[*].sourceId", contains("city")));
        search("free_text", "md").andExpect(jsonPath("$.listings[*].sourceId", contains("state")));
        search("free_text", "20001").andExpect(jsonPath("$.listings[*].sourceId", contains("zip")));
        search("free_text", "  rooftop pool ").andExpect(jsonPath("$.listings[*].sourceId", contains("desc")));
        search("free_text", "pool rooftop").andExpect(jsonPath("$.listings", hasSize(0)));
        search("free_text", "   ").andExpect(jsonPath("$.listings", hasSize(4)));
    }

    @Test
    void futureListingsAreExcluded() throws Exception {
        upload(List.of(with(listing("today"), entry("listedDate", "2026-11-25")),
                with(listing("tomorrow"), entry("listedDate", "2026-11-26"))));

        search().andExpect(jsonPath("$.listings[*].sourceId", contains("today")))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void fullRankingOrderEndToEnd() throws Exception {
        upload(List.of(
                at("sold-best", 100000, 38.7893, -77.1873, "2026-11-25", "sold"),
                at("pending", 150000, 38.80, -77.20, "2026-11-25", "pending"),
                at("active-far-pricey-old", 900000, 39.50, -78.00, "2026-08-01", "active"),
                at("active-near-cheap", 200000, 38.79, -77.19, "2026-11-24", "active"),
                at("active-mid", 500000, 39.00, -77.50, "2026-11-10", "active")));

        search().andExpect(jsonPath("$.listings[*].sourceId", contains(
                "active-near-cheap", "active-mid", "active-far-pricey-old", "pending", "sold-best")));
    }

    @Test
    void rankingIsAppliedBeforePagination() throws Exception {
        List<Object> batch = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            batch.add(at("l" + i, 100000 + i * 50000, 38.7893 + i * 0.1, -77.1873, "2026-11-25",
                    i % 3 == 0 ? "sold" : "active"));
        }
        upload(batch);

        JsonNode all = objectMapper.readTree(search("size", "100").andReturn().getResponse().getContentAsString());
        List<String> fullOrder = all.get("listings").findValuesAsText("sourceId");

        List<String> paged = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            JsonNode body = objectMapper.readTree(search("page", String.valueOf(page), "size", "3")
                    .andReturn().getResponse().getContentAsString());
            paged.addAll(body.get("listings").findValuesAsText("sourceId"));
            org.assertj.core.api.Assertions.assertThat(body.get("meta").get("totalPages").asInt()).isEqualTo(3);
        }
        org.assertj.core.api.Assertions.assertThat(paged).isEqualTo(fullOrder);
        org.assertj.core.api.Assertions.assertThat(fullOrder.subList(0, 4)).allMatch(id ->
                List.of("l1", "l2", "l4", "l5").contains(id));

        search("page", "5", "size", "3").andExpect(status().isOk())
                .andExpect(jsonPath("$.listings", hasSize(0)))
                .andExpect(jsonPath("$.meta.totalElements").value(7));
    }

    @Test
    void latAndLngChangeTheOrder() throws Exception {
        upload(List.of(at("north", 300000, 40.0, -77.0, "2026-11-25", "active"),
                at("south", 300000, 37.0, -77.0, "2026-11-25", "active")));

        search("lat", "40.0", "lng", "-77.0").andExpect(jsonPath("$.listings[*].sourceId",
                contains("north", "south")));
        search("lat", "37.0", "lng", "-77.0").andExpect(jsonPath("$.listings[*].sourceId",
                contains("south", "north")));
    }

    @Test
    void minPriceAboveMaxPriceReturns422() throws Exception {
        search("min_price", "500000", "max_price", "300000")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.search[0].field").value("min_price"))
                .andExpect(jsonPath("$.search[0].message")
                        .value("Validation Failure, min price (500000) is greater than max price (300000)"));
    }

    @Test
    void allParameterErrorsAreReportedTogether() throws Exception {
        search("min_price", "abc", "min_bedrooms", "-1", "min_bathrooms", "x", "lat", "38.9", "size", "0")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.search[*].field",
                        contains("min_price", "min_bedrooms", "min_bathrooms", "lng", "size")));
    }

    @Test
    void latWithoutLngReturns422() throws Exception {
        search("lat", "38.9").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.search[0].field").value("lng"));
        search("lng", "-77").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.search[0].field").value("lat"));
    }

    @Test
    void outOfRangeLocationAndPagingReturn422() throws Exception {
        search("lat", "91", "lng", "-181").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.search[*].field", contains("lat", "lng")));
        search("page", "-1").andExpect(status().isUnprocessableEntity());
        search("size", "101").andExpect(status().isUnprocessableEntity());
        search("min_price", "1.5").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void eachListingCarriesItsRankingScores() throws Exception {
        upload(List.of(
                at("here", 100000, 38.7893, -77.1873, "2026-11-25", "active"),
                at("north", 300000, 39.7893, -77.1873, "2026-11-18", "active")));

        search()
                .andExpect(jsonPath("$.listings[*].sourceId", contains("here", "north")))
                .andExpect(jsonPath("$.listings[0].ranking.score").value(10.0))
                .andExpect(jsonPath("$.listings[0].ranking.distanceScore").value(10.0))
                .andExpect(jsonPath("$.listings[0].ranking.priceScore").value(10.0))
                .andExpect(jsonPath("$.listings[0].ranking.freshnessScore").value(10.0))
                .andExpect(jsonPath("$.listings[0].ranking.distanceMiles").value(0.0))
                .andExpect(jsonPath("$.listings[0].ranking.ageDays").value(0))
                // 0.4 * 1 + 0.4 * 1 + 0.2 * 9 = 2.6
                .andExpect(jsonPath("$.listings[1].ranking.score").value(2.6))
                .andExpect(jsonPath("$.listings[1].ranking.distanceScore").value(1.0))
                .andExpect(jsonPath("$.listings[1].ranking.priceScore").value(1.0))
                .andExpect(jsonPath("$.listings[1].ranking.freshnessScore").value(9.0))
                .andExpect(jsonPath("$.listings[1].ranking.distanceMiles").value(69.0941))
                .andExpect(jsonPath("$.listings[1].ranking.ageDays").value(7));
    }

    @Test
    void rankingScoresUseTheWholeFilteredSetEvenOnLaterPages() throws Exception {
        upload(List.of(
                at("cheap", 100000, 38.7893, -77.1873, "2026-11-25", "active"),
                at("dear", 500000, 38.7893, -77.1873, "2026-11-25", "active")));

        // Page 1 holds only "dear", but its price score is still scaled against "cheap" (1, not 10).
        search("page", "1", "size", "1")
                .andExpect(jsonPath("$.listings[0].sourceId").value("dear"))
                .andExpect(jsonPath("$.listings[0].ranking.priceScore").value(1.0));
    }

    @Test
    void tiedScoresAreOrderedByNewerDateThenListingIdThroughTheApi() throws Exception {
        // Same place (each is the furthest, distance score 1), same price (10) and ages 0 and 3 (freshness 10):
        // all three score 0.4 * 1 + 0.4 * 10 + 0.2 * 10 = 6.4.
        upload(List.of(
                at("c-older", 300000, 38.9, -77.3, "2026-11-22", "active"),
                at("b-new", 300000, 38.9, -77.3, "2026-11-25", "active"),
                at("a-new", 300000, 38.9, -77.3, "2026-11-25", "active")));

        search()
                .andExpect(jsonPath("$.listings[*].sourceId", contains("a-new", "b-new", "c-older")))
                .andExpect(jsonPath("$.listings[*].ranking.score", contains(6.4, 6.4, 6.4)));

        // The tie order holds across a page boundary.
        search("page", "0", "size", "2")
                .andExpect(jsonPath("$.listings[*].sourceId", contains("a-new", "b-new")));
        search("page", "1", "size", "2")
                .andExpect(jsonPath("$.listings[*].sourceId", contains("c-older")));
    }
}
