package com.remark.platform.client;

import com.remark.platform.exceptions.InvalidMasterAssignmentException;
import com.remark.platform.security.RoleCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

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
        try {
            UserProfileResponse response = userServiceRestClient.get()
                    .uri("/internal/users/{userId}", userId)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .body(UserProfileResponse.class);
            if (response == null || !"ACTIVE".equals(response.status())) {
                throw new InvalidMasterAssignmentException("User is not active: " + userId);
            }
            return response;
        } catch (HttpClientErrorException.NotFound | HttpClientErrorException.Unauthorized ex) {
            throw new InvalidMasterAssignmentException("User not found or inactive: " + userId);
        }
    }

    public void ensureActiveMasterInBranch(UUID masterId, UUID branchId) {
        UserProfileResponse master = getActiveUser(masterId);
        if (!master.roles().contains(RoleCode.MASTER)) {
            throw new InvalidMasterAssignmentException("User is not a master: " + masterId);
        }
        if (master.branchId() == null || !master.branchId().equals(branchId)) {
            throw new InvalidMasterAssignmentException("Master must belong to the same branch as the remark");
        }
    }
}
