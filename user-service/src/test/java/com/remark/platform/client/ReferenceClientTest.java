package com.remark.platform.client;

import com.remark.platform.exceptions.InvalidRegistrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ReferenceClientTest {

    private static final UUID BRANCH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockRestServiceServer server;
    private ReferenceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://reference-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ReferenceClient(builder.build());
    }

    @Test
    void ensureActiveBranchExistsAcceptsActiveBranch() {
        server.expect(once(), requestTo("http://reference-service/api/v1/refs/branches/" + BRANCH_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "11111111-1111-1111-1111-111111111111",
                          "code": "BR-1",
                          "name": "Main",
                          "active": true
                        }
                        """, MediaType.APPLICATION_JSON));

        client.ensureActiveBranchExists(BRANCH_ID);

        server.verify();
    }

    @Test
    void ensureActiveBranchExistsRejectsInactiveBranch() {
        server.expect(once(), requestTo("http://reference-service/api/v1/refs/branches/" + BRANCH_ID))
                .andRespond(withSuccess("""
                        {
                          "id": "11111111-1111-1111-1111-111111111111",
                          "code": "BR-1",
                          "name": "Main",
                          "active": false
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.ensureActiveBranchExists(BRANCH_ID))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageContaining("inactive");
        server.verify();
    }

    @Test
    void ensureActiveBranchExistsConvertsNotFound() {
        server.expect(once(), requestTo("http://reference-service/api/v1/refs/branches/" + BRANCH_ID))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.ensureActiveBranchExists(BRANCH_ID))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessageContaining("Branch not found");
        server.verify();
    }
}
