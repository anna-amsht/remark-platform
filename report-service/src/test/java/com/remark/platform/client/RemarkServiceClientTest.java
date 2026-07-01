package com.remark.platform.client;

import com.remark.platform.dto.InternalRemarkResponse;
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

class RemarkServiceClientTest {

    private static final String INTERNAL_SECRET = "secret";

    private MockRestServiceServer server;
    private RemarkServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://remark-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RemarkServiceClient(builder.build(), INTERNAL_SECRET);
    }

    @Test
    void getRemarkReturnsRemarkFromRemarkService() {
        UUID remarkId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        server.expect(once(), requestTo("http://remark-service/internal/remarks/" + remarkId))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Secret", INTERNAL_SECRET))
                .andRespond(withSuccess("""
                        {
                          "id": "11111111-1111-1111-1111-111111111111",
                          "remarkNumber": "REM-2026-00001",
                          "branchId": "22222222-2222-2222-2222-222222222222",
                          "objectBuilding": "Main building",
                          "description": "Leak",
                          "discoveredAt": "2026-01-01",
                          "createdByUserId": "33333333-3333-3333-3333-333333333333",
                          "assignedMasterId": "44444444-4444-4444-4444-444444444444",
                          "assignedByUserId": "55555555-5555-5555-5555-555555555555",
                          "status": "RESOLVED",
                          "resolutionMessage": "Fixed",
                          "resolvedAt": "2026-01-02T10:00:00Z",
                          "reportSentAt": null
                        }
                        """, MediaType.APPLICATION_JSON));

        InternalRemarkResponse response = client.getRemark(remarkId);

        assertThat(response.remarkNumber()).isEqualTo("REM-2026-00001");
        assertThat(response.status()).isEqualTo("RESOLVED");
        server.verify();
    }

    @Test
    void getRemarkConvertsNotFoundToIllegalState() {
        UUID remarkId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        server.expect(once(), requestTo("http://remark-service/internal/remarks/" + remarkId))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.getRemark(remarkId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Remark not found");
        server.verify();
    }

    @Test
    void markReportSentSendsPatchRequest() {
        UUID remarkId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        server.expect(once(), requestTo("http://remark-service/internal/remarks/" + remarkId + "/report-sent"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-Internal-Secret", INTERNAL_SECRET))
                .andRespond(withSuccess());

        client.markReportSent(remarkId);

        server.verify();
    }
}
