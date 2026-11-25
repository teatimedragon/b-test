package com.somecompany.marketplace.admin;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.somecompany.marketplace.IntegrationTestSupport;
import com.somecompany.marketplace.domain.ListingEntity;
import com.somecompany.marketplace.domain.ListingLogEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AdminApiIntegrationTest extends IntegrationTestSupport {

    @Test
    void mixedBatchLogsEverythingSavesOnlyValidListingsAndCountsCorrectly() throws Exception {
        List<Object> batch = List.of(
                listing("1"),
                with(listing("2"), entry("price", "abc"), entry("state", "Virginia")),
                42,
                with(listing("3"), entry("state", "md"), entry("status", "PENDING")),
                with(listing("4"), new java.util.AbstractMap.SimpleEntry<>("id", null)));

        JsonNode result = upload(batch);

        assertThat(result.get("received").asInt()).isEqualTo(5);
        assertThat(result.get("accepted").asInt()).isEqualTo(2);
        assertThat(result.get("rejected").asInt()).isEqualTo(3);

        JsonNode results = result.get("results");
        assertThat(results.get(0).get("listingId").asText()).isEqualTo("zillow:1");
        assertThat(results.get(0).get("accepted").asBoolean()).isTrue();
        assertThat(results.get(0).get("errors")).isEmpty();

        assertThat(results.get(1).get("accepted").asBoolean()).isFalse();
        assertThat(results.get(1).get("errors").findValuesAsText("errorSource")).containsExactlyInAnyOrder(
                "Price", "State");
        assertThat(results.get(1).get("errors").findValuesAsText("errorDetails"))
                .contains("Price 'abc' is not a valid number", "State 'Virginia' must be exactly 2 letters");

        assertThat(results.get(2).get("listingId").isNull()).isTrue();
        assertThat(results.get(2).get("errors").get(0).get("errorSource").asText()).isEqualTo("general");

        assertThat(results.get(3).get("accepted").asBoolean()).isTrue();
        assertThat(results.get(4).get("listingId").isNull()).isTrue();
        assertThat(results.get(4).get("errors").get(0).get("errorSource").asText()).isEqualTo("SourceId");

        // Every entry is logged, one error row per failure, only clean listings saved.
        assertThat(logRepository.count()).isEqualTo(5);
        assertThat(errorRepository.count()).isEqualTo(4);
        assertThat(listingRepository.findAll()).extracting(ListingEntity::getListingId)
                .containsExactlyInAnyOrder("zillow:1", "zillow:3");

        ListingEntity saved = listingRepository.findById("zillow:3").orElseThrow();
        assertThat(saved.getState()).isEqualTo("MD");
        assertThat(saved.getStatus()).isEqualTo("pending");
        assertThat(saved.getSourceId()).isEqualTo("3");
        assertThat(saved.getSourceCode()).isEqualTo("zillow");
        assertThat(saved.getSquareFeet()).isEqualTo(1850);
        assertThat(saved.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void logStoresRawValuesTruncatedToFit() throws Exception {
        upload(List.of(with(listing("1"), entry("state", "Virginia"), entry("price", "abc"))));

        ListingLogEntity log = logRepository.findAll().get(0);
        assertThat(log.getState()).isEqualTo("Vi");
        assertThat(log.getPrice()).isEqualTo("abc");
        assertThat(log.getSourceId()).isEqualTo("1");
        assertThat(log.getSourceCode()).isEqualTo("zillow");
        assertThat(log.getSquareFeet()).isEqualTo("1850");
        assertThat(log.getCurrencyCode()).isEqualTo("USD");
    }

    @Test
    void uploadingAnExistingListingIdUpdatesIt() throws Exception {
        upload(List.of(listing("1")));
        upload(List.of(with(listing("1"), entry("price", 399000), entry("status", "sold"),
                entry("description", "Price reduced"))));

        assertThat(listingRepository.count()).isEqualTo(1);
        ListingEntity updated = listingRepository.findById("zillow:1").orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo("399000");
        assertThat(updated.getStatus()).isEqualTo("sold");
        assertThat(updated.getDescription()).isEqualTo("Price reduced");
        assertThat(logRepository.count()).isEqualTo(2);
    }

    @Test
    void sameListingIdTwiceInOneUploadLaterWins() throws Exception {
        upload(List.of(with(listing("1"), entry("price", 100000)), with(listing("1"), entry("price", 200000))));

        assertThat(listingRepository.count()).isEqualTo(1);
        assertThat(listingRepository.findById("zillow:1").orElseThrow().getPrice()).isEqualByComparingTo("200000");
    }

    @Test
    void numbersMayArriveAsStrings() throws Exception {
        JsonNode result = upload(List.of(with(listing("1"), entry("price", "450000.50"), entry("bedrooms", "0"),
                entry("latitude", "-33.8688"), entry("longitude", "151.2093"))));

        assertThat(result.get("accepted").asInt()).isEqualTo(1);
        ListingEntity saved = listingRepository.findById("zillow:1").orElseThrow();
        assertThat(saved.getPrice()).isEqualByComparingTo("450000.50");
        assertThat(saved.getBedrooms()).isEqualTo((short) 0);
        assertThat(saved.getLatitude()).isEqualByComparingTo("-33.8688");
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/marketplace-admin/listings").contentType(MediaType.APPLICATION_JSON).content("[{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("not valid JSON")));
        assertThat(logRepository.count()).isZero();
    }

    @Test
    void nonArrayBodyReturns400() throws Exception {
        mvc.perform(post("/marketplace-admin/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(listing("1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("JSON array")));
        assertThat(logRepository.count()).isZero();
    }

    @Test
    void emptyBodyReturns400() throws Exception {
        mvc.perform(post("/marketplace-admin/listings").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void emptyArrayIsAccepted() throws Exception {
        JsonNode result = upload(List.of());
        assertThat(result.get("received").asInt()).isZero();
        assertThat(result.get("results")).isEmpty();
    }

    @Test
    void logsAreMostRecentFirstAndIncludeErrors() throws Exception {
        upload(List.of(listing("first")));
        upload(List.of(with(listing("second"), entry("price", "abc"))));
        upload(List.of(listing("third")));

        mvc.perform(get("/marketplace-admin/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs[*].id", contains("third", "second", "first")))
                .andExpect(jsonPath("$.logs[0].accepted").value(true))
                .andExpect(jsonPath("$.logs[0].errors", hasSize(0)))
                .andExpect(jsonPath("$.logs[1].accepted").value(false))
                .andExpect(jsonPath("$.logs[1].price").value("abc"))
                .andExpect(jsonPath("$.logs[1].source").value("zillow"))
                .andExpect(jsonPath("$.logs[1].sqft").value("1850"))
                .andExpect(jsonPath("$.logs[1].currencyCode").value("USD"))
                .andExpect(jsonPath("$.logs[1].errors[0].errorId").isString())
                .andExpect(jsonPath("$.logs[1].errors[0].errorSource").value("Price"))
                .andExpect(jsonPath("$.logs[1].errors[0].errorDetails").value("Price 'abc' is not a valid number"))
                .andExpect(jsonPath("$.logs[0].logTime").value("2026-11-25T09:00:00.002Z"))
                .andExpect(jsonPath("$.meta.totalElements").value(3));
    }

    @Test
    void logsPaginate() throws Exception {
        upload(List.of(listing("a"), listing("b"), listing("c"), listing("d"), listing("e")));

        mvc.perform(get("/marketplace-admin/logs").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs[*].id", contains("c", "b")))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(2))
                .andExpect(jsonPath("$.meta.totalPages").value(3))
                .andExpect(jsonPath("$.meta.totalElements").value(5));

        mvc.perform(get("/marketplace-admin/logs").param("page", "9").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs", hasSize(0)))
                .andExpect(jsonPath("$.meta.totalPages").value(3))
                .andExpect(jsonPath("$.meta.totalElements").value(5));
    }

    @Test
    void listingsAreOrderedByListingIdAndPaginate() throws Exception {
        upload(List.of(listing("c"), listing("a"), listing("e"), listing("b"), listing("d")));

        mvc.perform(get("/marketplace-admin/listings").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings[*].listingId", contains("zillow:a", "zillow:b")))
                .andExpect(jsonPath("$.meta.totalPages").value(3))
                .andExpect(jsonPath("$.meta.totalElements").value(5));

        mvc.perform(get("/marketplace-admin/listings").param("page", "2").param("size", "2"))
                .andExpect(jsonPath("$.listings[*].listingId", contains("zillow:e")));

        mvc.perform(get("/marketplace-admin/listings").param("page", "3").param("size", "2"))
                .andExpect(jsonPath("$.listings", hasSize(0)))
                .andExpect(jsonPath("$.meta.page").value(3));
    }

    @Test
    void listingsUseUploadFieldNames() throws Exception {
        upload(List.of(listing("12345")));

        mvc.perform(get("/marketplace-admin/listings"))
                .andExpect(jsonPath("$.listings[0].listingId").value("zillow:12345"))
                .andExpect(jsonPath("$.listings[0].id").value("12345"))
                .andExpect(jsonPath("$.listings[0].source").value("zillow"))
                .andExpect(jsonPath("$.listings[0].sqft").value(1850))
                .andExpect(jsonPath("$.listings[0].price").value(450000.00))
                .andExpect(jsonPath("$.listings[0].bathrooms").value(2.5))
                .andExpect(jsonPath("$.listings[0].latitude").value(38.7893))
                .andExpect(jsonPath("$.listings[0].currencyCode").value("USD"))
                .andExpect(jsonPath("$.listings[0].status").value("active"))
                .andExpect(jsonPath("$.listings[0].sourceId").doesNotExist());
    }

    @Test
    void invalidPageParametersReturn422() throws Exception {
        mvc.perform(get("/marketplace-admin/logs").param("page", "-1").param("size", "101"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.search[*].field", contains("page", "size")));
        mvc.perform(get("/marketplace-admin/listings").param("size", "0"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.search[0].field").value("size"));
    }

    @Test
    void unknownPathKeepsItsStatusWithoutStackTrace() throws Exception {
        mvc.perform(get("/marketplace-admin/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void uploadResultHasNullListingIdWhenSourceMissing() throws Exception {
        JsonNode result = upload(List.of(with(listing("1"), entry("source", "  "))));
        assertThat(result.get("results").get(0).get("listingId").isNull()).isTrue();
        assertThat(result.get("results").get(0).get("logId").asText()).hasSize(36);
    }
}
