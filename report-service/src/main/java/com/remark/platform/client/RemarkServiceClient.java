package com.remark.platform.client;

import com.remark.platform.dto.InternalRemarkResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class RemarkServiceClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient remarkServiceRestClient;
    private final String internalSecret;

    public RemarkServiceClient(
            RestClient remarkServiceRestClient,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.remarkServiceRestClient = remarkServiceRestClient;
        this.internalSecret = internalSecret;
    }

    public InternalRemarkResponse getRemark(UUID remarkId) {
        try {
            InternalRemarkResponse response = remarkServiceRestClient.get()
                    .uri("/internal/remarks/{remarkId}", remarkId)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .body(InternalRemarkResponse.class);
            if (response == null) {
                throw new IllegalStateException("Remark not found: " + remarkId);
            }
            return response;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new IllegalStateException("Remark not found: " + remarkId);
        }
    }

    public void markReportSent(UUID remarkId) {
        remarkServiceRestClient.patch()
                .uri("/internal/remarks/{remarkId}/report-sent", remarkId)
                .header(INTERNAL_SECRET_HEADER, internalSecret)
                .retrieve()
                .toBodilessEntity();
    }
}
