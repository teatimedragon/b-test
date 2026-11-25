package com.somecompany.marketplace.marketplace;

import static com.somecompany.marketplace.marketplace.TestEntities.listing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.somecompany.marketplace.common.PageRequestParams;
import com.somecompany.marketplace.domain.ListingRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarketplaceSearchServiceTest {

    private final ListingRepository repository = mock(ListingRepository.class);
    private final MarketplaceSearchService service = new MarketplaceSearchService(repository,
            new ListingSearchFilter(), new ListingRanker(),
            Clock.fixed(Instant.parse("2026-11-25T23:59:00Z"), ZoneOffset.UTC));

    private static SearchCriteria page(int page, int size) {
        return new SearchCriteria(null, null, null, null, null, GeoPoint.DEFAULT_LOCATION,
                new PageRequestParams(page, size));
    }

    @Test
    void ranksTheWholeSetThenSlicesThePage() {
        when(repository.findListedOnOrBefore("2026-11-25")).thenReturn(List.of(
                listing("sold", "sold", "100", 38.7893, -77.1873, "2026-11-25"),
                listing("cheap", "active", "100", 38.7893, -77.1873, "2026-11-25"),
                listing("pricey", "active", "900", 38.7893, -77.1873, "2026-11-25")));

        MarketplaceSearchResponse first = service.search(page(0, 2));
        MarketplaceSearchResponse second = service.search(page(1, 2));

        assertThat(first.listings()).extracting(MarketplaceListingDto::sourceId).containsExactly("cheap", "pricey");
        assertThat(second.listings()).extracting(MarketplaceListingDto::sourceId).containsExactly("sold");
        assertThat(first.meta().totalElements()).isEqualTo(3);
        assertThat(first.meta().totalPages()).isEqualTo(2);
        verify(repository, org.mockito.Mockito.times(2)).findListedOnOrBefore("2026-11-25");
    }
}
