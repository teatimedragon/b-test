package com.somecompany.marketplace.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.somecompany.marketplace.common.FieldError;
import com.somecompany.marketplace.common.ParameterValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class SearchParamsValidatorTest {

    private final SearchParamsValidator validator = new SearchParamsValidator();

    private static SearchParamsValidator.RawParams params(String minPrice, String maxPrice, String minBeds,
            String minBaths, String text, String lat, String lng, String page, String size) {
        return new SearchParamsValidator.RawParams(minPrice, maxPrice, minBeds, minBaths, text, lat, lng, page, size);
    }

    private List<FieldError> errorsFor(SearchParamsValidator.RawParams raw) {
        try {
            validator.validate(raw);
        } catch (ParameterValidationException e) {
            return e.getErrors();
        }
        return List.of();
    }

    @Test
    void noParametersUseDefaults() {
        SearchCriteria c = validator.validate(params(null, null, null, null, null, null, null, null, null));
        assertThat(c.minPrice()).isNull();
        assertThat(c.maxPrice()).isNull();
        assertThat(c.minBedrooms()).isNull();
        assertThat(c.minBathrooms()).isNull();
        assertThat(c.freeText()).isNull();
        assertThat(c.location()).isEqualTo(new GeoPoint(38.7893, -77.1873));
        assertThat(c.page().page()).isZero();
        assertThat(c.page().size()).isEqualTo(20);
    }

    @Test
    void parsesAllValues() {
        SearchCriteria c = validator.validate(params("100", "500", "2", "1.5", "  Park View ", "40.1", "-75.2", "3",
                "50"));
        assertThat(c.minPrice()).isEqualTo(100L);
        assertThat(c.maxPrice()).isEqualTo(500L);
        assertThat(c.minBedrooms()).isEqualTo(2);
        assertThat(c.minBathrooms()).isEqualByComparingTo("1.5");
        assertThat(c.freeText()).isEqualTo("Park View");
        assertThat(c.location()).isEqualTo(new GeoPoint(40.1, -75.2));
        assertThat(c.page().page()).isEqualTo(3);
        assertThat(c.page().size()).isEqualTo(50);
    }

    @Test
    void blankFreeTextIsIgnored() {
        assertThat(validator.validate(params(null, null, null, null, "   ", null, null, null, null)).freeText())
                .isNull();
    }

    @Test
    void minGreaterThanMaxFails() {
        assertThat(errorsFor(params("500000", "300000", null, null, null, null, null, null, null)))
                .containsExactly(new FieldError("min_price",
                        "Validation Failure, min price (500000) is greater than max price (300000)"));
    }

    @Test
    void minEqualToMaxIsAllowed() {
        assertThat(errorsFor(params("300000", "300000", null, null, null, null, null, null, null))).isEmpty();
    }

    @Test
    void negativesFail() {
        assertThat(errorsFor(params("-1", "-2", "-1", "-0.5", null, null, null, null, null)))
                .extracting(FieldError::field)
                .containsExactly("min_price", "max_price", "min_bedrooms", "min_bathrooms");
    }

    @Test
    void zeroIsAllowed() {
        assertThat(errorsFor(params("0", "0", "0", "0", null, null, null, null, null))).isEmpty();
    }

    @Test
    void nonNumericValuesFail() {
        assertThat(errorsFor(params("abc", "1.5", "two", "x", null, "north", "1", "p", "s")))
                .extracting(FieldError::field)
                .containsExactly("min_price", "max_price", "min_bedrooms", "min_bathrooms", "lat", "page", "size");
    }

    @Test
    void latWithoutLngFails() {
        assertThat(errorsFor(params(null, null, null, null, null, "38.9", null, null, null)))
                .extracting(FieldError::field).containsExactly("lng");
        assertThat(errorsFor(params(null, null, null, null, null, null, "-77", null, null)))
                .extracting(FieldError::field).containsExactly("lat");
    }

    @Test
    void latAndLngRanges() {
        assertThat(errorsFor(params(null, null, null, null, null, "90", "180", null, null))).isEmpty();
        assertThat(errorsFor(params(null, null, null, null, null, "-90", "-180", null, null))).isEmpty();
        assertThat(errorsFor(params(null, null, null, null, null, "90.5", "-180.1", null, null)))
                .extracting(FieldError::field).containsExactly("lat", "lng");
    }

    @Test
    void pageAndSizeLimits() {
        assertThat(errorsFor(params(null, null, null, null, null, null, null, "0", "1"))).isEmpty();
        assertThat(errorsFor(params(null, null, null, null, null, null, null, "0", "100"))).isEmpty();
        assertThat(errorsFor(params(null, null, null, null, null, null, null, "-1", "0")))
                .extracting(FieldError::field).containsExactly("page", "size");
        assertThat(errorsFor(params(null, null, null, null, null, null, null, null, "101")))
                .extracting(FieldError::field).containsExactly("size");
    }

    @Test
    void reportsAllErrorsTogether() {
        assertThatThrownBy(() -> validator.validate(params("9", "1", "-1", null, null, "1", null, "-1", "500")))
                .isInstanceOfSatisfying(ParameterValidationException.class, e ->
                        assertThat(e.getErrors()).extracting(FieldError::field)
                                .containsExactly("min_price", "min_bedrooms", "lng", "page", "size"));
    }
}
