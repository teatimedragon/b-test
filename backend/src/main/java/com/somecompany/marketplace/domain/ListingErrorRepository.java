package com.somecompany.marketplace.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingErrorRepository extends JpaRepository<ListingErrorEntity, UUID> {

    @Query("select e from ListingErrorEntity e where e.log.logId in :logIds order by e.errorId")
    List<ListingErrorEntity> findByLogIds(@Param("logIds") Collection<UUID> logIds);

    @Query("select count(e) from ListingErrorEntity e where e.log.logId = :logId")
    long countByLogId(@Param("logId") UUID logId);
}
