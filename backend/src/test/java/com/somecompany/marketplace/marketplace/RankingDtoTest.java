package com.somecompany.marketplace.marketplace;

import static com.somecompany.marketplace.marketplace.TestEntities.listing;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RankingDtoTest {

    @Test
    void namesEachScoreAndRoundsToFourDecimals() {
        RankedListing ranked = new RankedListing(listing("a", "active", "1", 0, 0, "2026-11-25"),
                12.345678, 3, 7.123456789, 2.5, 9, 6.03456);

        RankingDto dto = RankingDto.from(ranked);

        assertThat(dto.score()).hasToString("6.0346");
        assertThat(dto.distanceScore()).hasToString("7.1235");
        assertThat(dto.priceScore()).hasToString("2.5000");
        assertThat(dto.freshnessScore()).hasToString("9.0000");
        assertThat(dto.distanceMiles()).hasToString("12.3457");
        assertThat(dto.ageDays()).isEqualTo(3);
    }
}
