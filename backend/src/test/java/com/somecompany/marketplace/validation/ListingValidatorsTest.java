package com.somecompany.marketplace.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ListingValidatorsTest {

    private static List<ValidationError> run(ListingValidator validator, UnaryOperator<RawListing.Builder> change) {
        return validator.apply(change.apply(TestListings.valid()).build());
    }

    private static void assertSingleError(List<ValidationError> errors, String source, String... fragments) {
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorSource()).isEqualTo(source);
        assertThat(errors.get(0).errorDetails()).contains(fragments);
    }

    @Test
    void validListingPassesEveryValidator() {
        RawListing listing = TestListings.valid().build();
        for (ListingField field : ListingField.values()) {
            assertThat(ListingValidators.required(field).apply(listing)).isEmpty();
            assertThat(ListingValidators.maxLength(field).apply(listing)).isEmpty();
        }
        assertThat(ListingValidators.state().apply(listing)).isEmpty();
        assertThat(ListingValidators.status().apply(listing)).isEmpty();
        assertThat(ListingValidators.listedDate().apply(listing)).isEmpty();
        assertThat(ListingValidators.price().apply(listing)).isEmpty();
        assertThat(ListingValidators.bedrooms().apply(listing)).isEmpty();
        assertThat(ListingValidators.bathrooms().apply(listing)).isEmpty();
        assertThat(ListingValidators.sqft().apply(listing)).isEmpty();
        assertThat(ListingValidators.latitude().apply(listing)).isEmpty();
        assertThat(ListingValidators.longitude().apply(listing)).isEmpty();
        assertThat(ListingValidators.listingIdLength().apply(listing)).isEmpty();
    }

    @Nested
    class Required {

        @ParameterizedTest
        @EnumSource(ListingField.class)
        void missingValueFails(ListingField field) {
            RawListing listing = withField(field, null);
            assertSingleError(ListingValidators.required(field).apply(listing), field.column(), "required", "null");
        }

        @ParameterizedTest
        @EnumSource(ListingField.class)
        void emptyValueFails(ListingField field) {
            RawListing listing = withField(field, "");
            assertSingleError(ListingValidators.required(field).apply(listing), field.column(), "''", "empty");
        }

        @ParameterizedTest
        @EnumSource(ListingField.class)
        void whitespaceOnlyValueFails(ListingField field) {
            RawListing listing = withField(field, "   ");
            assertSingleError(ListingValidators.required(field).apply(listing), field.column(), "'   '", "blank");
        }
    }

    @Nested
    class MaxLength {

        @ParameterizedTest
        @EnumSource(value = ListingField.class, names = {"ID", "SOURCE", "ADDRESS", "CITY", "ZIP", "LISTED_DATE",
                "STATUS", "DESCRIPTION"})
        void valueAtTheLimitPasses(ListingField field) {
            assertThat(ListingValidators.maxLength(field).apply(withField(field, "x".repeat(field.maxLength()))))
                    .isEmpty();
        }

        @ParameterizedTest
        @EnumSource(ListingField.class)
        void valueOverTheLimitFails(ListingField field) {
            String value = "y".repeat(field.maxLength() + 1);
            assertSingleError(ListingValidators.maxLength(field).apply(withField(field, value)), field.column(),
                    "'y", (field.maxLength() + 1) + " characters", "max is " + field.maxLength());
        }

        @Test
        void columnSizesMatchTheSpec() {
            assertThat(ListingField.ID.maxLength()).isEqualTo(50);
            assertThat(ListingField.SOURCE.maxLength()).isEqualTo(100);
            assertThat(ListingField.STATE.maxLength()).isEqualTo(2);
            assertThat(ListingField.DESCRIPTION.maxLength()).isEqualTo(250);
            assertThat(ListingField.LATITUDE.maxLength()).isEqualTo(20);
            assertThat(ListingField.LONGITUDE.maxLength()).isEqualTo(20);
        }
    }

    @Nested
    class ListingIdLength {

        @Test
        void exactly150CharactersPasses() {
            // 100 + ":" + 49 = 150
            assertThat(run(ListingValidators.listingIdLength(), b -> b.source("s".repeat(100)).id("i".repeat(49))))
                    .isEmpty();
        }

        @Test
        void over150CharactersIsAGeneralError() {
            assertSingleError(
                    run(ListingValidators.listingIdLength(), b -> b.source("s".repeat(100)).id("i".repeat(50))),
                    "general", "151 characters", "max is 150");
        }

        @Test
        void skippedWhenIdOrSourceMissing() {
            assertThat(run(ListingValidators.listingIdLength(), b -> b.id(null))).isEmpty();
            assertThat(run(ListingValidators.listingIdLength(), b -> b.source(" "))).isEmpty();
        }
    }

    @Nested
    class State {

        @ParameterizedTest
        @ValueSource(strings = {"VA", "va", "Md", " ny "})
        void twoLettersPass(String value) {
            assertThat(run(ListingValidators.state(), b -> b.state(value))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Virginia", "V", "V1", "12", "V-"})
        void otherValuesFail(String value) {
            assertSingleError(run(ListingValidators.state(), b -> b.state(value)), "State",
                    "'" + value + "'", "must be exactly 2 letters");
        }

        @Test
        void specExampleMessage() {
            assertThat(run(ListingValidators.state(), b -> b.state("Virginia")).get(0).errorDetails())
                    .isEqualTo("State 'Virginia' must be exactly 2 letters");
        }

        @Test
        void missingIsLeftToRequired() {
            assertThat(run(ListingValidators.state(), b -> b.state(null))).isEmpty();
        }
    }

    @Nested
    class Status {

        @ParameterizedTest
        @ValueSource(strings = {"active", "pending", "sold", "ACTIVE", "Pending", "SoLd"})
        void knownStatusesPassCaseInsensitively(String value) {
            assertThat(run(ListingValidators.status(), b -> b.status(value))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"closed", "for sale", "activ"})
        void otherStatusesFail(String value) {
            assertSingleError(run(ListingValidators.status(), b -> b.status(value)), "Status",
                    "'" + value + "'", "must be active, pending or sold");
        }
    }

    @Nested
    class ListedDate {

        @ParameterizedTest
        @ValueSource(strings = {"2026-11-20", "2024-02-29", "1999-01-01"})
        void isoDatesPass(String value) {
            assertThat(run(ListingValidators.listedDate(), b -> b.listedDate(value))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-13-01", "2026-02-30", "2025-02-29", "20-11-2026", "2026/11/20", "2026-1-5",
                "2026-11-20T10:00:00", "yesterday"})
        void invalidDatesFail(String value) {
            assertSingleError(run(ListingValidators.listedDate(), b -> b.listedDate(value)), "ListedDate",
                    "'" + value + "'", "yyyy-MM-dd");
        }
    }

    @Nested
    class Price {

        @ParameterizedTest
        @ValueSource(strings = {"0.0001", "1", "450000", "450000.00", "123456789012345.1234", "4.5E+5"})
        void validPricesPass(String value) {
            assertThat(run(ListingValidators.price(), b -> b.price(value))).isEmpty();
        }

        @Test
        void zeroFails() {
            assertSingleError(run(ListingValidators.price(), b -> b.price("0")), "Price", "'0'",
                    "must be greater than 0");
        }

        @Test
        void negativeFailsWithSpecMessage() {
            assertThat(run(ListingValidators.price(), b -> b.price("-5")))
                    .extracting(ValidationError::errorDetails)
                    .containsExactly("Price '-5' must be greater than 0");
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "12a", "NaN", "Infinity", "true", "{}"})
        void nonNumbersFail(String value) {
            assertSingleError(run(ListingValidators.price(), b -> b.price(value)), "Price",
                    "'" + value + "'", "is not a valid number");
        }

        @Test
        void tooManyDigitsFail() {
            assertSingleError(run(ListingValidators.price(), b -> b.price("1234567890123456")), "Price",
                    "15 digits before");
            assertSingleError(run(ListingValidators.price(), b -> b.price("1.12345")), "Price",
                    "4 decimal places");
        }

        @Test
        void missingIsLeftToRequired() {
            assertThat(run(ListingValidators.price(), b -> b.price(null))).isEmpty();
        }
    }

    @Nested
    class Bedrooms {

        @ParameterizedTest
        @ValueSource(strings = {"0", "1", "3", "3.0", "32767"})
        void validCountsPass(String value) {
            assertThat(run(ListingValidators.bedrooms(), b -> b.bedrooms(value))).isEmpty();
        }

        @Test
        void zeroIsAStudio() {
            assertThat(run(ListingValidators.bedrooms(), b -> b.bedrooms("0"))).isEmpty();
        }

        @Test
        void negativeFails() {
            assertSingleError(run(ListingValidators.bedrooms(), b -> b.bedrooms("-1")), "Bedrooms", "'-1'",
                    "0 or greater");
        }

        @Test
        void fractionFails() {
            assertSingleError(run(ListingValidators.bedrooms(), b -> b.bedrooms("2.5")), "Bedrooms", "'2.5'",
                    "whole number");
        }

        @Test
        void overMaxFails() {
            assertSingleError(run(ListingValidators.bedrooms(), b -> b.bedrooms("32768")), "Bedrooms", "'32768'",
                    "at most 32767");
        }

        @Test
        void wrongTypeFails() {
            assertSingleError(run(ListingValidators.bedrooms(), b -> b.bedrooms("three")), "Bedrooms", "'three'",
                    "not a valid number");
        }
    }

    @Nested
    class Bathrooms {

        @ParameterizedTest
        @ValueSource(strings = {"0.5", "1", "2.5", "2.25", "9999.99"})
        void validValuesPass(String value) {
            assertThat(run(ListingValidators.bathrooms(), b -> b.bathrooms(value))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-1"})
        void zeroOrNegativeFails(String value) {
            assertSingleError(run(ListingValidators.bathrooms(), b -> b.bathrooms(value)), "Bathrooms",
                    "'" + value + "'", "greater than 0");
        }

        @Test
        void tooManyDecimalsFail() {
            assertSingleError(run(ListingValidators.bathrooms(), b -> b.bathrooms("2.125")), "Bathrooms",
                    "'2.125'", "2 decimal places");
        }

        @Test
        void tooLargeForColumnFails() {
            assertSingleError(run(ListingValidators.bathrooms(), b -> b.bathrooms("10000")), "Bathrooms",
                    "less than 10000");
        }

        @Test
        void wrongTypeFails() {
            assertSingleError(run(ListingValidators.bathrooms(), b -> b.bathrooms("two")), "Bathrooms", "'two'",
                    "not a valid number");
        }
    }

    @Nested
    class SquareFeet {

        @ParameterizedTest
        @ValueSource(strings = {"1", "1850", "2147483647"})
        void validValuesPass(String value) {
            assertThat(run(ListingValidators.sqft(), b -> b.sqft(value))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "-10"})
        void zeroOrNegativeFails(String value) {
            assertSingleError(run(ListingValidators.sqft(), b -> b.sqft(value)), "SquareFeet", "'" + value + "'",
                    "greater than 0");
        }

        @Test
        void fractionFails() {
            assertSingleError(run(ListingValidators.sqft(), b -> b.sqft("1850.5")), "SquareFeet", "whole number");
        }

        @Test
        void overIntegerMaxFails() {
            assertSingleError(run(ListingValidators.sqft(), b -> b.sqft("2147483648")), "SquareFeet", "at most");
        }

        @Test
        void wrongTypeFails() {
            assertSingleError(run(ListingValidators.sqft(), b -> b.sqft("big")), "SquareFeet", "'big'",
                    "not a valid number");
        }
    }

    @Nested
    class Latitude {

        @ParameterizedTest
        @ValueSource(strings = {"-90", "90", "0", "-45.5", "38.7893", "38.12345678"})
        void inRangeValuesPassIncludingNegativeAndZero(String value) {
            assertThat(run(ListingValidators.latitude(), b -> b.latitude(value))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"-90.000001", "90.1", "180"})
        void outOfRangeFails(String value) {
            assertSingleError(run(ListingValidators.latitude(), b -> b.latitude(value)), "Latitude",
                    "'" + value + "'", "between -90 and 90");
        }

        @ParameterizedTest
        @ValueSource(strings = {"north", "NaN", "Infinity"})
        void nonFiniteOrTextFails(String value) {
            assertSingleError(run(ListingValidators.latitude(), b -> b.latitude(value)), "Latitude",
                    "not a valid number");
        }
    }

    @Nested
    class Longitude {

        @ParameterizedTest
        @ValueSource(strings = {"-180", "180", "0", "-77.1873"})
        void inRangeValuesPassIncludingNegativeAndZero(String value) {
            assertThat(run(ListingValidators.longitude(), b -> b.longitude(value))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"-180.5", "181"})
        void outOfRangeFails(String value) {
            assertSingleError(run(ListingValidators.longitude(), b -> b.longitude(value)), "Longitude",
                    "'" + value + "'", "between -180 and 180");
        }

        @Test
        void textFails() {
            assertSingleError(run(ListingValidators.longitude(), b -> b.longitude("west")), "Longitude",
                    "not a valid number");
        }
    }

    @Test
    void longValuesAreShortenedInMessagesAndMessagesFitTheColumn() {
        String longValue = "z".repeat(400);
        List<ValidationError> errors = run(ListingValidators.maxLength(ListingField.DESCRIPTION),
                b -> b.description(longValue));
        assertThat(errors.get(0).errorDetails()).hasSizeLessThanOrEqualTo(100).contains("...");
    }

    private static RawListing withField(ListingField field, String value) {
        RawListing.Builder b = TestListings.valid();
        switch (field) {
            case ID -> b.id(value);
            case SOURCE -> b.source(value);
            case ADDRESS -> b.address(value);
            case CITY -> b.city(value);
            case STATE -> b.state(value);
            case ZIP -> b.zip(value);
            case PRICE -> b.price(value);
            case BEDROOMS -> b.bedrooms(value);
            case BATHROOMS -> b.bathrooms(value);
            case SQFT -> b.sqft(value);
            case LATITUDE -> b.latitude(value);
            case LONGITUDE -> b.longitude(value);
            case LISTED_DATE -> b.listedDate(value);
            case STATUS -> b.status(value);
            case DESCRIPTION -> b.description(value);
        }
        return b.build();
    }
}
