package com.remark.platform.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserServiceClientTest {

    private static final String INTERNAL_SECRET = "secret";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BRANCH_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private MockRestServiceServer server;
    private UserServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UserServiceClient(builder.build(), INTERNAL_SECRET);
    }

    @Test
    void getActiveUserReturnsUserProfile() {
        server.expect(once(), requestTo("http://user-service/internal/users/" + USER_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Secret", INTERNAL_SECRET))
                .andRespond(withSuccess(userJson(USER_ID, BRANCH_ID, "MASTER"), MediaType.APPLICATION_JSON));

        UserServiceClient.UserProfileResponse response = client.getActiveUser(USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.roles()).containsExactly("MASTER");
        server.verify();
    }

    @Test
    void getActiveUserRejectsEmptyBody() {
        server.expect(once(), requestTo("http://user-service/internal/users/" + USER_ID))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getActiveUser(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found");
        server.verify();
    }

    @Test
    void findActiveUsersByBranchAndRoleReturnsProfiles() {
        server.expect(once(), requestTo("http://user-service/internal/users?branchId=" + BRANCH_ID + "&role=MASTER"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Secret", INTERNAL_SECRET))
                .andRespond(withSuccess("[" + userJson(USER_ID, BRANCH_ID, "MASTER") + "]", MediaType.APPLICATION_JSON));

        List<UserServiceClient.UserProfileResponse> response = client.findActiveUsersByBranchAndRole(BRANCH_ID, "MASTER");

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().branchId()).isEqualTo(BRANCH_ID);
        server.verify();
    }

    private static String userJson(UUID userId, UUID branchId, String role) {
        return """
                {
                  "userId": "%s",
                  "email": "user@example.com",
                  "fullName": "User",
                  "position": "Engineer",
                  "branchId": "%s",
                  "status": "ACTIVE",
                  "roles": ["%s"]
                }
                """.formatted(userId, branchId, role);
    }
}
