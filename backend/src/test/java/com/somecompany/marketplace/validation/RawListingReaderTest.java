package com.somecompany.marketplace.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

class RawListingReaderTest {

    private final ObjectMapper json = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
    private final RawListingReader reader = new RawListingReader();

    private JsonNode parse(String text) throws Exception {
        return json.readTree(text);
    }

    @Test
    void convertsStringsNumbersAndNulls() throws Exception {
        RawListing raw = reader.read(parse("""
                {"id": 12345, "source": "zillow", "price": 450000.10, "bedrooms": "3", "bathrooms": 2.5,
                 "sqft": null, "latitude": -77.000001, "status": "active"}
                """));

        assertThat(raw.jsonObject()).isTrue();
        assertThat(raw.id()).isEqualTo("12345");
        assertThat(raw.source()).isEqualTo("zillow");
        assertThat(raw.price()).isEqualTo("450000.10");
        assertThat(raw.bedrooms()).isEqualTo("3");
        assertThat(raw.bathrooms()).isEqualTo("2.5");
        assertThat(raw.sqft()).isNull();
        assertThat(raw.latitude()).isEqualTo("-77.000001");
        assertThat(raw.address()).isNull();
    }

    @Test
    void otherJsonTypesBecomeTheirJsonText() throws Exception {
        RawListing raw = reader.read(parse("{\"price\": true, \"city\": {\"a\":1}, \"zip\": [1]}"));
        assertThat(raw.price()).isEqualTo("true");
        assertThat(raw.city()).isEqualTo("{\"a\":1}");
        assertThat(raw.zip()).isEqualTo("[1]");
    }

    @Test
    void largeNumbersKeepPlainForm() throws Exception {
        RawListing raw = reader.read(parse("{\"price\": 1E+5, \"sqft\": 123456789012345678901}"));
        assertThat(raw.price()).isEqualTo("100000");
        assertThat(raw.sqft()).isEqualTo("123456789012345678901");
    }

    @Test
    void nonObjectEntryIsMarked() throws Exception {
        RawListing raw = reader.read(parse("42"));
        assertThat(raw.jsonObject()).isFalse();
        assertThat(raw.rawJson()).isEqualTo("42");
        assertThat(raw.id()).isNull();
    }
}
