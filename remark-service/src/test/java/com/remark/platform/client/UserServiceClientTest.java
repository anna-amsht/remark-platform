package com.remark.platform.client;

import com.remark.platform.exceptions.InvalidMasterAssignmentException;
import com.remark.platform.security.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserServiceClientTest {

    private static final String INTERNAL_SECRET = "secret";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BRANCH_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OTHER_BRANCH_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private MockRestServiceServer server;
    private UserServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UserServiceClient(builder.build(), INTERNAL_SECRET);
    }

    @Test
    void getActiveUserReturnsActiveUser() {
        expectUser(USER_ID, userJson(USER_ID, BRANCH_ID, "ACTIVE", "MASTER"));

        UserProfileResponse response = client.getActiveUser(USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.roles()).containsExactly(RoleCode.MASTER);
        server.verify();
    }

    @Test
    void getActiveUserRejectsInactiveUser() {
        expectUser(USER_ID, userJson(USER_ID, BRANCH_ID, "BLOCKED", "MASTER"));

        assertThatThrownBy(() -> client.getActiveUser(USER_ID))
                .isInstanceOf(InvalidMasterAssignmentException.class)
                .hasMessageContaining("not active");
        server.verify();
    }

    @Test
    void getActiveUserConvertsNotFound() {
        server.expect(once(), requestTo("http://user-service/internal/users/" + USER_ID))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getActiveUser(USER_ID))
                .isInstanceOf(InvalidMasterAssignmentException.class)
                .hasMessageContaining("not found");
        server.verify();
    }

    @Test
    void ensureActiveMasterInBranchAcceptsMasterFromSameBranch() {
        expectUser(USER_ID, userJson(USER_ID, BRANCH_ID, "ACTIVE", "MASTER"));

        client.ensureActiveMasterInBranch(USER_ID, BRANCH_ID);

        server.verify();
    }

    @Test
    void ensureActiveMasterInBranchRejectsNonMaster() {
        expectUser(USER_ID, userJson(USER_ID, BRANCH_ID, "ACTIVE", "APPLICANT"));

        assertThatThrownBy(() -> client.ensureActiveMasterInBranch(USER_ID, BRANCH_ID))
                .isInstanceOf(InvalidMasterAssignmentException.class)
                .hasMessageContaining("not a master");
        server.verify();
    }

    @Test
    void ensureActiveMasterInBranchRejectsMasterFromOtherBranch() {
        expectUser(USER_ID, userJson(USER_ID, OTHER_BRANCH_ID, "ACTIVE", "MASTER"));

        assertThatThrownBy(() -> client.ensureActiveMasterInBranch(USER_ID, BRANCH_ID))
                .isInstanceOf(InvalidMasterAssignmentException.class)
                .hasMessageContaining("same branch");
        server.verify();
    }

    private void expectUser(UUID userId, String body) {
        server.expect(once(), requestTo("http://user-service/internal/users/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Secret", INTERNAL_SECRET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private static String userJson(UUID userId, UUID branchId, String status, String role) {
        return """
                {
                  "userId": "%s",
                  "email": "user@example.com",
                  "fullName": "User",
                  "position": "Engineer",
                  "branchId": "%s",
                  "status": "%s",
                  "roles": ["%s"]
                }
                """.formatted(userId, branchId, status, role);
    }
}
