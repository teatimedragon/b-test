package com.somecompany.marketplace.admin;

import com.somecompany.marketplace.domain.ListingErrorEntity;
import com.somecompany.marketplace.domain.ListingLogEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A ListingLog entry with its raw string values and its errors. */
public record LogDto(
        UUID logId,
        Instant logTime,
        String id,
        String source,
        String address,
        String city,
        String state,
        String zip,
        String price,
        String currencyCode,
        String bedrooms,
        String bathrooms,
        String sqft,
        String latitude,
        String longitude,
        String listedDate,
        String status,
        String description,
        boolean accepted,
        List<ErrorDto> errors) {

    public record ErrorDto(UUID errorId, String errorSource, String errorDetails) {
    }

    public static LogDto from(ListingLogEntity l, List<ListingErrorEntity> errors) {
        List<ErrorDto> errorDtos = errors.stream()
                .map(e -> new ErrorDto(e.getErrorId(), e.getErrorSource(), e.getErrorDetails()))
                .toList();
        return new LogDto(l.getLogId(), l.getLogTime(), l.getSourceId(), l.getSourceCode(), l.getAddress(),
                l.getCity(), l.getState(), l.getZip(), l.getPrice(), l.getCurrencyCode(), l.getBedrooms(),
                l.getBathrooms(), l.getSquareFeet(), l.getLatitude(), l.getLongitude(), l.getListedDate(),
                l.getStatus(), l.getDescription(), errorDtos.isEmpty(), errorDtos);
    }
}
