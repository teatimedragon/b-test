package com.somecompany.marketplace.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationPipelineTest {

    private final ValidationPipeline pipeline = ValidationPipeline.standard();

    @Test
    void cleanListingHasNoErrors() {
        assertThat(pipeline.validate(TestListings.valid().build())).isEmpty();
    }

    @Test
    void collectsErrorsFromEveryValidatorInsteadOfStoppingAtTheFirst() {
        RawListing listing = TestListings.valid()
                .price("abc")
                .state("Virginia")
                .status("closed")
                .latitude("100")
                .bedrooms("-1")
                .city(null)
                .build();

        assertThat(pipeline.validate(listing))
                .extracting(ValidationError::errorSource)
                .containsExactlyInAnyOrder("Price", "State", "Status", "Latitude", "Bedrooms", "City");
    }

    @Test
    void missingFieldGivesOneErrorNotOnePerRule() {
        assertThat(pipeline.validate(TestListings.valid().price(null).build()))
                .extracting(ValidationError::errorSource)
                .containsExactly("Price");
    }

    @Test
    void runsValidatorsInOrder() {
        List<String> calls = new ArrayList<>();
        ValidationPipeline custom = new ValidationPipeline(List.of(
                l -> { calls.add("a"); return List.of(new ValidationError("A", "a failed")); },
                l -> { calls.add("b"); return List.of(); },
                l -> { calls.add("c"); return List.of(new ValidationError("C", "c failed")); }));

        assertThat(custom.validate(TestListings.valid().build()))
                .extracting(ValidationError::errorSource)
                .containsExactly("A", "C");
        assertThat(calls).containsExactly("a", "b", "c");
    }

    @Test
    void nonObjectEntryGetsOneGeneralError() {
        List<ValidationError> errors = pipeline.validate(RawListing.notAnObject("42"));
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).errorSource()).isEqualTo("general");
        assertThat(errors.get(0).errorDetails()).contains("'42'", "not a JSON object");
    }

    @Test
    void errorDetailsAreTruncatedTo100Characters() {
        assertThat(new ValidationError("Price", "x".repeat(150)).errorDetails()).hasSize(100);
    }
}
