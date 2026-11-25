package com.somecompany.marketplace.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingLogRepository extends JpaRepository<ListingLogEntity, UUID> {
}
