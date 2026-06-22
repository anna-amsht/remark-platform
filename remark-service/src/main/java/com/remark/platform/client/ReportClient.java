package com.remark.platform.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class ReportClient {

    private static final Logger log = LoggerFactory.getLogger(ReportClient.class);
    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient reportServiceRestClient;
    private final String internalSecret;

    public ReportClient(
            RestClient reportServiceRestClient,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.reportServiceRestClient = reportServiceRestClient;
        this.internalSecret = internalSecret;
    }

    public void requestReport(UUID remarkId) {
        try {
            reportServiceRestClient.post()
                    .uri("/internal/reports/generate")
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .body(new GenerateReportRequest(remarkId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to request remark report: {}", ex.getMessage());
        }
    }

    private record GenerateReportRequest(UUID remarkId) {
    }
}
