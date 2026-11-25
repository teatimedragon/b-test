package com.somecompany.marketplace.admin;

import com.somecompany.marketplace.common.PageMeta;
import java.util.List;

public record AdminListingsResponse(List<AdminListingDto> listings, PageMeta meta) {
}
