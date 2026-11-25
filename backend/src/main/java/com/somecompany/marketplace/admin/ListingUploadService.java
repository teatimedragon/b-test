package com.somecompany.marketplace.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.somecompany.marketplace.common.IdGenerator;
import com.somecompany.marketplace.domain.ListingErrorEntity;
import com.somecompany.marketplace.domain.ListingErrorRepository;
import com.somecompany.marketplace.domain.ListingLogEntity;
import com.somecompany.marketplace.domain.ListingLogRepository;
import com.somecompany.marketplace.domain.ListingRepository;
import com.somecompany.marketplace.validation.ListingMapper;
import com.somecompany.marketplace.validation.RawListing;
import com.somecompany.marketplace.validation.RawListingReader;
import com.somecompany.marketplace.validation.ValidationError;
import com.somecompany.marketplace.validation.ValidationPipeline;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Processes an uploaded array: for each entry, log it, validate it, record its errors and upsert it
 * into Listing when clean. Each entry runs in its own transaction, so one failure never stops or
 * rolls back the others.
 */
@Service
public class ListingUploadService {

    private static final Logger log = LoggerFactory.getLogger(ListingUploadService.class);

    private final ListingLogRepository logRepository;
    private final ListingErrorRepository errorRepository;
    private final ListingRepository listingRepository;
    private final RawListingReader reader;
    private final ValidationPipeline pipeline;
    private final ListingMapper mapper;
    private final IdGenerator ids;
    private final Clock clock;
    private final TransactionTemplate tx;

    public ListingUploadService(ListingLogRepository logRepository, ListingErrorRepository errorRepository,
            ListingRepository listingRepository, RawListingReader reader, ValidationPipeline pipeline,
            ListingMapper mapper, IdGenerator ids, Clock clock, TransactionTemplate tx) {
        this.logRepository = logRepository;
        this.errorRepository = errorRepository;
        this.listingRepository = listingRepository;
        this.reader = reader;
        this.pipeline = pipeline;
        this.mapper = mapper;
        this.ids = ids;
        this.clock = clock;
        this.tx = tx;
    }

    public UploadResult upload(JsonNode array) {
        List<UploadResult.Item> results = new ArrayList<>();
        for (JsonNode entry : array) {
            results.add(processOne(reader.read(entry)));
        }
        int accepted = (int) results.stream().filter(UploadResult.Item::accepted).count();
        return new UploadResult(results.size(), accepted, results.size() - accepted, results);
    }

    UploadResult.Item processOne(RawListing raw) {
        String listingId = mapper.listingIdOf(raw);
        try {
            return tx.execute(status -> {
                // 1. Log first.
                ListingLogEntity entry = logRepository.save(mapper.toLog(raw, ids.next(), now()));
                // 2. Validate.
                List<ValidationError> errors = pipeline.validate(raw);
                // 3. Record errors.
                saveErrors(entry, errors);
                // 4. Save if clean (insert or update every column).
                if (errors.isEmpty()) {
                    listingRepository.save(mapper.toListing(raw));
                }
                return item(entry, listingId, errors);
            });
        } catch (RuntimeException ex) {
            log.error("Unexpected error while processing listing {}", listingId, ex);
            List<ValidationError> errors = List.of(new ValidationError(ValidationError.GENERAL,
                    "Unexpected error while saving the listing"));
            return tx.execute(status -> {
                ListingLogEntity entry = logRepository.save(mapper.toLog(raw, ids.next(), now()));
                saveErrors(entry, errors);
                return item(entry, listingId, errors);
            });
        }
    }

    private void saveErrors(ListingLogEntity entry, List<ValidationError> errors) {
        for (ValidationError error : errors) {
            errorRepository.save(new ListingErrorEntity(ids.next(), entry, error.errorSource(), error.errorDetails()));
        }
    }

    private static UploadResult.Item item(ListingLogEntity entry, String listingId, List<ValidationError> errors) {
        return new UploadResult.Item(entry.getLogId(), listingId, errors.isEmpty(),
                errors.stream().map(e -> new UploadResult.Error(e.errorSource(), e.errorDetails())).toList());
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
