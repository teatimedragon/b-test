package com.somecompany.marketplace.admin;

import java.util.List;
import java.util.UUID;

/** Response of {@code POST /marketplace-admin/listings}. */
public record UploadResult(int received, int accepted, int rejected, List<Item> results) {

    public record Item(UUID logId, String listingId, boolean accepted, List<Error> errors) {
    }

    public record Error(String errorSource, String errorDetails) {
    }
}
