package com.somecompany.marketplace.marketplace;

import com.somecompany.marketplace.common.Decimals;
import com.somecompany.marketplace.domain.ListingEntity;
import java.math.BigDecimal;

/** A listing in the public marketplace API. */
public record MarketplaceListingDto(
        String listingId,
        String sourceId,
        String sourceCode,
        String address,
        String city,
        String state,
        String zip,
        BigDecimal price,
        String currencyCode,
        Integer bedrooms,
        BigDecimal bathrooms,
        Integer sqft,
        BigDecimal latitude,
        BigDecimal longitude,
        String listedDate,
        String status,
        String description,
        RankingDto ranking) {

    /** A ranked listing, with the scores that decided its position. */
    public static MarketplaceListingDto from(RankedListing ranked) {
        return from(ranked.listing(), RankingDto.from(ranked));
    }

    public static MarketplaceListingDto from(ListingEntity l, RankingDto ranking) {
        return new MarketplaceListingDto(l.getListingId(), l.getSourceId(), l.getSourceCode(), l.getAddress(),
                l.getCity(), l.getState(), l.getZip(), Decimals.price(l.getPrice()), l.getCurrencyCode(),
                l.getBedrooms() == null ? null : l.getBedrooms().intValue(), Decimals.compact(l.getBathrooms()),
                l.getSquareFeet(), l.getLatitude(), l.getLongitude(), l.getListedDate(), l.getStatus(),
                l.getDescription(), ranking);
    }
}
