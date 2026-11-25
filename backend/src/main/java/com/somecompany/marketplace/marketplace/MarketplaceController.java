package com.somecompany.marketplace.marketplace;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/marketplace")
public class MarketplaceController {

    private final MarketplaceSearchService searchService;
    private final SearchParamsValidator paramsValidator;

    public MarketplaceController(MarketplaceSearchService searchService, SearchParamsValidator paramsValidator) {
        this.searchService = searchService;
        this.paramsValidator = paramsValidator;
    }

    /** Parameters are taken as strings so every problem can be reported together as a 422. */
    @GetMapping("/listing")
    public MarketplaceSearchResponse search(
            @RequestParam(name = "min_price", required = false) String minPrice,
            @RequestParam(name = "max_price", required = false) String maxPrice,
            @RequestParam(name = "min_bedrooms", required = false) String minBedrooms,
            @RequestParam(name = "min_bathrooms", required = false) String minBathrooms,
            @RequestParam(name = "free_text", required = false) String freeText,
            @RequestParam(name = "lat", required = false) String lat,
            @RequestParam(name = "lng", required = false) String lng,
            @RequestParam(name = "page", required = false) String page,
            @RequestParam(name = "size", required = false) String size) {
        SearchCriteria criteria = paramsValidator.validate(new SearchParamsValidator.RawParams(
                minPrice, maxPrice, minBedrooms, minBathrooms, freeText, lat, lng, page, size));
        return searchService.search(criteria);
    }
}
