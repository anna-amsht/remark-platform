package com.remark.platform.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class ReferenceServiceClient {

    private final RestClient referenceServiceRestClient;

    public ReferenceServiceClient(@Value("${app.reference-service.base-url}") String baseUrl) {
        this.referenceServiceRestClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public BranchResponse getBranch(UUID branchId) {
        BranchResponse response = referenceServiceRestClient.get()
                .uri("/api/v1/refs/branches/{branchId}", branchId)
                .retrieve()
                .body(BranchResponse.class);
        if (response == null) {
            throw new IllegalStateException("Branch not found: " + branchId);
        }
        return response;
    }

    public record BranchResponse(UUID id, String code, String name, boolean active) {
    }
}
