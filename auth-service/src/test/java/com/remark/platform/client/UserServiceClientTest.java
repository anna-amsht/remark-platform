package com.remark.platform.client;

import com.remark.platform.exceptions.InvalidCredentialsException;
import com.remark.platform.exceptions.InvalidRefreshTokenException;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class UserServiceClientTest {

    private static final String INTERNAL_SECRET = "secret";

    private MockRestServiceServer server;
    private UserServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://user-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new UserServiceClient(builder.build(), INTERNAL_SECRET);
    }

    @Test
    void authenticateReturnsUserFromUserService() {
        server.expect(once(), requestTo("http://user-service/internal/users/authenticate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Secret", INTERNAL_SECRET))
                .andRespond(withSuccess("""
                        {
                          "userId": "11111111-1111-1111-1111-111111111111",
                          "email": "user@example.com",
                          "fullName": "User",
                          "position": "Engineer",
                          "branchId": "22222222-2222-2222-2222-222222222222",
                          "status": "ACTIVE",
                          "roles": ["APPLICANT"]
                        }
                        """, MediaType.APPLICATION_JSON));

        AuthenticatedUserResponse response = client.authenticate("user@example.com", "password");

        assertThat(response.userId()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(response.roles()).containsExactly(RoleCode.APPLICANT);
        server.verify();
    }

    @Test
    void authenticateConvertsUnauthorizedToInvalidCredentials() {
        server.expect(once(), requestTo("http://user-service/internal/users/authenticate"))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.authenticate("user@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
        server.verify();
    }

    @Test
    void authenticateConvertsEmptyBodyToInvalidCredentials() {
        server.expect(once(), requestTo("http://user-service/internal/users/authenticate"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.authenticate("user@example.com", "password"))
                .isInstanceOf(InvalidCredentialsException.class);
        server.verify();
    }

    @Test
    void getActiveUserByIdReturnsUser() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        server.expect(once(), requestTo("http://user-service/internal/users/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Secret", INTERNAL_SECRET))
                .andRespond(withSuccess("""
                        {
                          "userId": "11111111-1111-1111-1111-111111111111",
                          "email": "user@example.com",
                          "fullName": "User",
                          "position": "Engineer",
                          "branchId": "22222222-2222-2222-2222-222222222222",
                          "status": "ACTIVE",
                          "roles": ["MASTER"]
                        }
                        """, MediaType.APPLICATION_JSON));

        AuthenticatedUserResponse response = client.getActiveUserById(userId);

        assertThat(response.roles()).containsExactly(RoleCode.MASTER);
        server.verify();
    }

    @Test
    void getActiveUserByIdConvertsNotFoundToInvalidRefreshToken() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        server.expect(once(), requestTo("http://user-service/internal/users/" + userId))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getActiveUserById(userId))
                .isInstanceOf(InvalidRefreshTokenException.class);
        server.verify();
    }

    @Test
    void getActiveUserByIdConvertsForbiddenToInvalidRefreshToken() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        server.expect(once(), requestTo("http://user-service/internal/users/" + userId))
                .andRespond(withForbiddenRequest());

        assertThatThrownBy(() -> client.getActiveUserById(userId))
                .isInstanceOf(InvalidRefreshTokenException.class);
        server.verify();
    }
}
