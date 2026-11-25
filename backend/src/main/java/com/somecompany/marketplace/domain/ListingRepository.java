package com.somecompany.marketplace.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ListingRepository extends JpaRepository<ListingEntity, String> {

    /** Listings dated today or earlier. ListedDate is always stored as yyyy-MM-dd, so string order is date order. */
    @Query("select l from ListingEntity l where l.listedDate <= :today")
    List<ListingEntity> findListedOnOrBefore(@Param("today") String today);
}
