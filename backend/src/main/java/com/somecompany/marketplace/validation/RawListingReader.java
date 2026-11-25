package com.somecompany.marketplace.validation;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Converts one uploaded JSON entry to a {@link RawListing}. Strings are kept as is, numbers become
 * their plain decimal text, JSON null or a missing field becomes {@code null}, and anything else
 * (booleans, objects, arrays) becomes its JSON text so it can be logged and then rejected.
 */
public class RawListingReader {

    public RawListing read(JsonNode node) {
        if (node == null || !node.isObject()) {
            return RawListing.notAnObject(node == null ? "null" : node.toString());
        }
        return RawListing.builder()
                .id(text(node, "id"))
                .source(text(node, "source"))
                .address(text(node, "address"))
                .city(text(node, "city"))
                .state(text(node, "state"))
                .zip(text(node, "zip"))
                .price(text(node, "price"))
                .bedrooms(text(node, "bedrooms"))
                .bathrooms(text(node, "bathrooms"))
                .sqft(text(node, "sqft"))
                .latitude(text(node, "latitude"))
                .longitude(text(node, "longitude"))
                .listedDate(text(node, "listedDate"))
                .status(text(node, "status"))
                .description(text(node, "description"))
                .build();
    }

    static String text(JsonNode parent, String name) {
        JsonNode value = parent.get(name);
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        if (value.isTextual()) {
            return value.textValue();
        }
        if (value.isNumber()) {
            if (value.isBigDecimal() || value.isFloatingPointNumber()) {
                return value.decimalValue().toPlainString();
            }
            return value.bigIntegerValue().toString();
        }
        return value.toString();
    }
}
