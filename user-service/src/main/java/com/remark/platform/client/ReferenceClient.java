package com.remark.platform.client;

import com.remark.platform.exceptions.InvalidRegistrationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class ReferenceClient {

    private final RestClient referenceRestClient;

    public ReferenceClient(RestClient referenceRestClient) {
        this.referenceRestClient = referenceRestClient;
    }

    public void ensureActiveBranchExists(UUID branchId) {
        try {
            BranchResponse branch = referenceRestClient.get()
                    .uri("/api/v1/refs/branches/{id}", branchId)
                    .retrieve()
                    .body(BranchResponse.class);
            if (branch == null || !branch.active()) {
                throw new InvalidRegistrationException("Branch not found or inactive: " + branchId);
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new InvalidRegistrationException("Branch not found: " + branchId);
        }
    }
}
