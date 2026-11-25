package com.somecompany.marketplace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.somecompany.marketplace.domain.ListingErrorRepository;
import com.somecompany.marketplace.domain.ListingLogRepository;
import com.somecompany.marketplace.domain.ListingRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ListingRepository listingRepository;

    @Autowired
    protected ListingLogRepository logRepository;

    @Autowired
    protected ListingErrorRepository errorRepository;

    @Autowired
    protected TestClockConfig.MutableClock clock;

    @BeforeEach
    void resetDatabaseAndClock() {
        errorRepository.deleteAll();
        logRepository.deleteAll();
        listingRepository.deleteAll();
        clock.set(TestClockConfig.START);
    }

    /** A valid upload entry; override fields with {@code with}. */
    protected static Map<String, Object> listing(String id) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("source", "zillow");
        m.put("address", "123 Main St");
        m.put("city", "Alexandria");
        m.put("state", "VA");
        m.put("zip", "22306");
        m.put("price", 450000);
        m.put("bedrooms", 3);
        m.put("bathrooms", 2.5);
        m.put("sqft", 1850);
        m.put("latitude", 38.7893);
        m.put("longitude", -77.1873);
        m.put("listedDate", "2026-11-20");
        m.put("status", "active");
        m.put("description", "Bright colonial near the park.");
        return m;
    }

    @SafeVarargs
    protected static Map<String, Object> with(Map<String, Object> base, Map.Entry<String, Object>... changes) {
        Map<String, Object> copy = new LinkedHashMap<>(base);
        for (Map.Entry<String, Object> change : changes) {
            copy.put(change.getKey(), change.getValue());
        }
        return copy;
    }

    protected JsonNode upload(Object body) throws Exception {
        String json = body instanceof String s ? s : objectMapper.writeValueAsString(body);
        String response = mvc.perform(post("/marketplace-admin/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}
