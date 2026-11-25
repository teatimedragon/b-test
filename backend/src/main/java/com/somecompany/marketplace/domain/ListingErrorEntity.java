package com.somecompany.marketplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** One validation failure for one ListingLog entry. */
@Entity
@Table(name = "ListingError")
public class ListingErrorEntity {

    @Id
    @Column(name = "ErrorId", nullable = false)
    private UUID errorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LogId", nullable = false)
    private ListingLogEntity log;

    @Column(name = "ErrorSource", length = 50, nullable = false)
    private String errorSource;

    @Column(name = "ErrorDetails", length = 100, nullable = false)
    private String errorDetails;

    public ListingErrorEntity() {
    }

    public ListingErrorEntity(UUID errorId, ListingLogEntity log, String errorSource, String errorDetails) {
        this.errorId = errorId;
        this.log = log;
        this.errorSource = errorSource;
        this.errorDetails = errorDetails;
    }

    public UUID getErrorId() { return errorId; }
    public ListingLogEntity getLog() { return log; }
    public UUID getLogId() { return log.getLogId(); }
    public String getErrorSource() { return errorSource; }
    public String getErrorDetails() { return errorDetails; }
}
