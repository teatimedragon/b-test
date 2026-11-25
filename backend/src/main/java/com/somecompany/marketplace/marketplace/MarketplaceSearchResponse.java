package com.somecompany.marketplace.marketplace;

import com.somecompany.marketplace.common.PageMeta;
import java.util.List;

public record MarketplaceSearchResponse(List<MarketplaceListingDto> listings, PageMeta meta) {
}
