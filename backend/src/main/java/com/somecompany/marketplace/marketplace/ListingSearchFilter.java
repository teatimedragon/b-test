package com.somecompany.marketplace.marketplace;

import com.somecompany.marketplace.domain.ListingEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/** Applies the search filters (all combined with AND) and drops future-dated listings. */
public class ListingSearchFilter {

    public List<ListingEntity> filter(List<ListingEntity> listings, SearchCriteria criteria, LocalDate today) {
        return listings.stream().filter(predicate(criteria, today)).toList();
    }

    public Predicate<ListingEntity> predicate(SearchCriteria c, LocalDate today) {
        String phrase = c.freeText() == null ? null : c.freeText().toLowerCase(Locale.ROOT);
        return l -> !isFuture(l, today)
                && (c.minPrice() == null || l.getPrice().compareTo(BigDecimal.valueOf(c.minPrice())) >= 0)
                && (c.maxPrice() == null || l.getPrice().compareTo(BigDecimal.valueOf(c.maxPrice())) <= 0)
                && (c.minBedrooms() == null || l.getBedrooms() >= c.minBedrooms())
                && (c.minBathrooms() == null || l.getBathrooms().compareTo(c.minBathrooms()) >= 0)
                && (phrase == null || matchesText(l, phrase));
    }

    private static boolean isFuture(ListingEntity l, LocalDate today) {
        return LocalDate.parse(l.getListedDate()).isAfter(today);
    }

    /** The whole phrase as a case-insensitive substring of city, state, zip or description. */
    private static boolean matchesText(ListingEntity l, String lowerPhrase) {
        return contains(l.getCity(), lowerPhrase)
                || contains(l.getState(), lowerPhrase)
                || contains(l.getZip(), lowerPhrase)
                || contains(l.getDescription(), lowerPhrase);
    }

    private static boolean contains(String value, String lowerPhrase) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerPhrase);
    }
}
