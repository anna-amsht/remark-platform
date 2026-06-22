package com.remark.platform.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Set;
import java.util.UUID;

@Component
public class UserServiceClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient userServiceRestClient;
    private final String internalSecret;

    public UserServiceClient(
            RestClient userServiceRestClient,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.userServiceRestClient = userServiceRestClient;
        this.internalSecret = internalSecret;
    }

    public UserProfileResponse getActiveUser(UUID userId) {
        UserProfileResponse response = userServiceRestClient.get()
                .uri("/internal/users/{userId}", userId)
                .header(INTERNAL_SECRET_HEADER, internalSecret)
                .retrieve()
                .body(UserProfileResponse.class);
        if (response == null) {
            throw new IllegalStateException("User not found: " + userId);
        }
        return response;
    }

    public record UserProfileResponse(
            UUID userId,
            String email,
            String fullName,
            String position,
            UUID branchId,
            String status,
            Set<String> roles
    ) {
    }
}
