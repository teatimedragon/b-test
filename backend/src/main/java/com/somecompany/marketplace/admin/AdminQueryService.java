package com.somecompany.marketplace.admin;

import com.somecompany.marketplace.common.PageMeta;
import com.somecompany.marketplace.common.PageRequestParams;
import com.somecompany.marketplace.domain.ListingEntity;
import com.somecompany.marketplace.domain.ListingErrorEntity;
import com.somecompany.marketplace.domain.ListingErrorRepository;
import com.somecompany.marketplace.domain.ListingLogEntity;
import com.somecompany.marketplace.domain.ListingLogRepository;
import com.somecompany.marketplace.domain.ListingRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of the admin API: the Listing table and the ListingLog with its errors. */
@Service
@Transactional(readOnly = true)
public class AdminQueryService {

    private final ListingRepository listingRepository;
    private final ListingLogRepository logRepository;
    private final ListingErrorRepository errorRepository;

    public AdminQueryService(ListingRepository listingRepository, ListingLogRepository logRepository,
            ListingErrorRepository errorRepository) {
        this.listingRepository = listingRepository;
        this.logRepository = logRepository;
        this.errorRepository = errorRepository;
    }

    public AdminListingsResponse listings(PageRequestParams params) {
        Page<ListingEntity> page = listingRepository.findAll(
                PageRequest.of(params.page(), params.size(), Sort.by("listingId").ascending()));
        return new AdminListingsResponse(
                page.getContent().stream().map(AdminListingDto::from).toList(),
                PageMeta.of(params.page(), params.size(), page.getTotalElements()));
    }

    public LogsResponse logs(PageRequestParams params) {
        // Most recent first; the time-ordered UUIDv7 LogId breaks ties within the same instant.
        Page<ListingLogEntity> page = logRepository.findAll(PageRequest.of(params.page(), params.size(),
                Sort.by(Sort.Order.desc("logTime"), Sort.Order.desc("logId"))));
        List<UUID> logIds = page.getContent().stream().map(ListingLogEntity::getLogId).toList();
        Map<UUID, List<ListingErrorEntity>> errorsByLog = logIds.isEmpty()
                ? Map.of()
                : errorRepository.findByLogIds(logIds).stream()
                        .collect(Collectors.groupingBy(ListingErrorEntity::getLogId));
        List<LogDto> logs = page.getContent().stream()
                .map(entry -> LogDto.from(entry, errorsByLog.getOrDefault(entry.getLogId(), List.of())))
                .toList();
        return new LogsResponse(logs, PageMeta.of(params.page(), params.size(), page.getTotalElements()));
    }
}
