package com.somecompany.marketplace.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.somecompany.marketplace.common.UuidV7Generator;
import com.somecompany.marketplace.domain.ListingEntity;
import com.somecompany.marketplace.domain.ListingErrorEntity;
import com.somecompany.marketplace.domain.ListingErrorRepository;
import com.somecompany.marketplace.domain.ListingLogEntity;
import com.somecompany.marketplace.domain.ListingLogRepository;
import com.somecompany.marketplace.domain.ListingRepository;
import com.somecompany.marketplace.validation.ListingMapper;
import com.somecompany.marketplace.validation.RawListingReader;
import com.somecompany.marketplace.validation.ValidationPipeline;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class ListingUploadServiceTest {

    private final ObjectMapper json = new ObjectMapper();
    private ListingLogRepository logs;
    private ListingErrorRepository errors;
    private ListingRepository listings;
    private ListingUploadService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        logs = mock(ListingLogRepository.class);
        errors = mock(ListingErrorRepository.class);
        listings = mock(ListingRepository.class);
        TransactionTemplate tx = mock(TransactionTemplate.class);
        when(tx.execute(any())).thenAnswer(inv -> ((TransactionCallback<Object>) inv.getArgument(0))
                .doInTransaction(null));
        when(logs.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new ListingUploadService(logs, errors, listings, new RawListingReader(),
                ValidationPipeline.standard(), new ListingMapper(), new UuidV7Generator(),
                Clock.fixed(Instant.parse("2026-11-25T09:00:00Z"), ZoneOffset.UTC), tx);
    }

    private static String valid(String id) {
        return """
                {"id":"%s","source":"zillow","address":"1 Main St","city":"Alexandria","state":"VA","zip":"22306",
                 "price":450000,"bedrooms":3,"bathrooms":2,"sqft":1500,"latitude":38.8,"longitude":-77.1,
                 "listedDate":"2026-11-20","status":"active","description":"Nice"}
                """.formatted(id);
    }

    @Test
    void logsBeforeValidatingAndSavesCleanListing() throws Exception {
        UploadResult result = service.upload(json.readTree("[" + valid("1") + "]"));

        assertThat(result.accepted()).isEqualTo(1);
        verify(logs).save(any(ListingLogEntity.class));
        verify(errors, never()).save(any());
        verify(listings).save(argThat((ListingEntity l) -> l.getListingId().equals("zillow:1")));
    }

    @Test
    void invalidListingWritesOneErrorRowPerFailureAndIsNotSaved() throws Exception {
        String bad = valid("1").replace("\"price\":450000", "\"price\":\"abc\"").replace("\"VA\"", "\"Virginia\"");
        UploadResult result = service.upload(json.readTree("[" + bad + "]"));

        assertThat(result.rejected()).isEqualTo(1);
        verify(errors, times(2)).save(any(ListingErrorEntity.class));
        verify(listings, never()).save(any());
    }

    @Test
    void unexpectedFailureOnOneListingDoesNotStopTheOthers() throws Exception {
        when(listings.save(argThat((ListingEntity l) -> l != null && l.getListingId().equals("zillow:1"))))
                .thenThrow(new IllegalStateException("database down"));

        UploadResult result = service.upload(json.readTree("[" + valid("1") + "," + valid("2") + "]"));

        assertThat(result.received()).isEqualTo(2);
        assertThat(result.accepted()).isEqualTo(1);
        assertThat(result.results().get(0).accepted()).isFalse();
        assertThat(result.results().get(0).errors().get(0).errorSource()).isEqualTo("general");
        assertThat(result.results().get(1).accepted()).isTrue();
    }
}
