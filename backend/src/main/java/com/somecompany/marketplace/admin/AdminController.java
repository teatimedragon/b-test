package com.somecompany.marketplace.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.somecompany.marketplace.common.MalformedRequestException;
import com.somecompany.marketplace.common.PageRequestParams;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/marketplace-admin")
public class AdminController {

    private final ListingUploadService uploadService;
    private final AdminQueryService queryService;
    private final ObjectReader jsonReader;

    public AdminController(ListingUploadService uploadService, AdminQueryService queryService,
            ObjectMapper objectMapper) {
        this.uploadService = uploadService;
        this.queryService = queryService;
        // Keep decimals exact (e.g. 450000.10 stays 450000.10, not a double).
        this.jsonReader = objectMapper.reader()
                .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .with(JsonNodeFactory.withExactBigDecimals(true));
    }

    @GetMapping("/listings")
    public AdminListingsResponse listings(@RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        return queryService.listings(PageRequestParams.parseOrThrow(page, size));
    }

    @GetMapping("/logs")
    public LogsResponse logs(@RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        return queryService.logs(PageRequestParams.parseOrThrow(page, size));
    }

    /** The body is read as text and parsed here so malformed JSON and non-arrays get a clear 400. */
    @PostMapping("/listings")
    public UploadResult upload(@RequestBody(required = false) String body) {
        if (body == null || body.isBlank()) {
            throw new MalformedRequestException("Request body must be a JSON array of listings");
        }
        JsonNode root;
        try {
            root = jsonReader.readTree(body);
        } catch (JsonProcessingException e) {
            throw new MalformedRequestException("Request body is not valid JSON");
        }
        if (root == null || !root.isArray()) {
            throw new MalformedRequestException("Request body must be a JSON array of listings");
        }
        return uploadService.upload(root);
    }
}
