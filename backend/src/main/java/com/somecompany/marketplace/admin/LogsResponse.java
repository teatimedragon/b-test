package com.somecompany.marketplace.admin;

import com.somecompany.marketplace.common.PageMeta;
import java.util.List;

public record LogsResponse(List<LogDto> logs, PageMeta meta) {
}
