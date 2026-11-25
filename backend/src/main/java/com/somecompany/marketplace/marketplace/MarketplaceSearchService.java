package com.somecompany.marketplace.marketplace;

import com.somecompany.marketplace.common.PageMeta;
import com.somecompany.marketplace.domain.ListingEntity;
import com.somecompany.marketplace.domain.ListingRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Filter, then rank the whole filtered set, then paginate. */
@Service
@Transactional(readOnly = true)
public class MarketplaceSearchService {

    private final ListingRepository repository;
    private final ListingSearchFilter filter;
    private final ListingRanker ranker;
    private final Clock clock;

    public MarketplaceSearchService(ListingRepository repository, ListingSearchFilter filter, ListingRanker ranker,
            Clock clock) {
        this.repository = repository;
        this.filter = filter;
        this.ranker = ranker;
        this.clock = clock;
    }

    public MarketplaceSearchResponse search(SearchCriteria criteria) {
        LocalDate today = LocalDate.now(clock);
        List<ListingEntity> candidates = repository.findListedOnOrBefore(today.toString());
        List<ListingEntity> filtered = filter.filter(candidates, criteria, today);
        List<RankedListing> ranked = ranker.rank(filtered, criteria.location(), today);
        List<MarketplaceListingDto> page = criteria.page().slice(ranked).stream()
                .map(MarketplaceListingDto::from)
                .toList();
        return new MarketplaceSearchResponse(page,
                PageMeta.of(criteria.page().page(), criteria.page().size(), ranked.size()));
    }
}
