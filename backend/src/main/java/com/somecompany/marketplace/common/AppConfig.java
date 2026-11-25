package com.somecompany.marketplace.common;

import com.somecompany.marketplace.marketplace.ListingRanker;
import com.somecompany.marketplace.marketplace.ListingSearchFilter;
import com.somecompany.marketplace.marketplace.SearchParamsValidator;
import com.somecompany.marketplace.validation.ListingMapper;
import com.somecompany.marketplace.validation.RawListingReader;
import com.somecompany.marketplace.validation.ValidationPipeline;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the framework-free business classes as Spring beans. The classes themselves have no
 * Spring dependencies, so they can be unit tested with plain constructors.
 */
@Configuration
public class AppConfig {

    /** "Today" for freshness and future-date filtering. UTC by default; tests replace it. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public IdGenerator idGenerator() {
        return new UuidV7Generator();
    }

    @Bean
    public ValidationPipeline validationPipeline() {
        return ValidationPipeline.standard();
    }

    @Bean
    public RawListingReader rawListingReader() {
        return new RawListingReader();
    }

    @Bean
    public ListingMapper listingMapper() {
        return new ListingMapper();
    }

    @Bean
    public ListingRanker listingRanker() {
        return new ListingRanker();
    }

    @Bean
    public ListingSearchFilter listingSearchFilter() {
        return new ListingSearchFilter();
    }

    @Bean
    public SearchParamsValidator searchParamsValidator() {
        return new SearchParamsValidator();
    }
}
