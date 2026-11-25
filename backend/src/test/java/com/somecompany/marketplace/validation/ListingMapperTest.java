package com.somecompany.marketplace.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.somecompany.marketplace.domain.ListingEntity;
import com.somecompany.marketplace.domain.ListingLogEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListingMapperTest {

    private final ListingMapper mapper = new ListingMapper();

    @Test
    void mapsRequestFieldsToColumns() {
        ListingEntity l = mapper.toListing(TestListings.valid().build());

        assertThat(l.getSourceId()).isEqualTo("12345");          // id -> SourceId
        assertThat(l.getSourceCode()).isEqualTo("zillow");       // source -> SourceCode
        assertThat(l.getSquareFeet()).isEqualTo(1850);           // sqft -> SquareFeet
        assertThat(l.getListingId()).isEqualTo("zillow:12345");  // SourceCode:SourceId
        assertThat(l.getAddress()).isEqualTo("123 Main St");
        assertThat(l.getCity()).isEqualTo("Alexandria");
        assertThat(l.getZip()).isEqualTo("22306");
        assertThat(l.getPrice()).isEqualByComparingTo("450000");
        assertThat(l.getPrice().scale()).isEqualTo(4);
        assertThat(l.getBedrooms()).isEqualTo((short) 3);
        assertThat(l.getBathrooms()).isEqualByComparingTo("2.5");
        assertThat(l.getCurrencyCode()).isEqualTo("USD");
        assertThat(l.getListedDate()).isEqualTo("2026-11-20");
        assertThat(l.getDescription()).isEqualTo("Bright colonial near the park.");
    }

    @Test
    void normalisesStateStatusAndWhitespace() {
        ListingEntity l = mapper.toListing(TestListings.valid().state("va").status("PENDING").city("  Alexandria ")
                .build());
        assertThat(l.getState()).isEqualTo("VA");
        assertThat(l.getStatus()).isEqualTo("pending");
        assertThat(l.getCity()).isEqualTo("Alexandria");
    }

    @Test
    void roundsCoordinatesToSixDecimals() {
        ListingEntity l = mapper.toListing(TestListings.valid().latitude("38.12345678").longitude("-77.1234565")
                .build());
        assertThat(l.getLatitude()).isEqualTo(new BigDecimal("38.123457"));
        assertThat(l.getLongitude()).isEqualTo(new BigDecimal("-77.123457"));
    }

    @Test
    void listingIdIsNullWhenIdOrSourceMissing() {
        assertThat(mapper.listingIdOf(TestListings.valid().id(null).build())).isNull();
        assertThat(mapper.listingIdOf(TestListings.valid().source("").build())).isNull();
        assertThat(ListingMapper.listingIdOf(" zillow ", " 1 ")).isEqualTo("zillow:1");
    }

    @Test
    void logKeepsRawStringsAndTruncatesToColumnSize() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-25T09:15:02.123Z");
        RawListing raw = TestListings.valid()
                .price("abc")
                .state("Virginia")
                .latitude("38.1234567890123456789012")
                .description("d".repeat(300))
                .build();

        ListingLogEntity log = mapper.toLog(raw, id, now);

        assertThat(log.getLogId()).isEqualTo(id);
        assertThat(log.getLogTime()).isEqualTo(now);
        assertThat(log.getSourceId()).isEqualTo("12345");
        assertThat(log.getSourceCode()).isEqualTo("zillow");
        assertThat(log.getSquareFeet()).isEqualTo("1850");
        assertThat(log.getPrice()).isEqualTo("abc");
        assertThat(log.getState()).isEqualTo("Vi");
        assertThat(log.getLatitude()).hasSize(20);
        assertThat(log.getDescription()).hasSize(250);
        assertThat(log.getCurrencyCode()).isEqualTo("USD");
    }
}
