package com.remark.platform.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NotificationClientTest {

    private static final String INTERNAL_SECRET = "secret";

    private MockRestServiceServer server;
    private NotificationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://notification-service");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NotificationClient(builder.build(), INTERNAL_SECRET);
    }

    @Test
    void sendReportNotificationPostsInternalNotification() {
        UUID recipientUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID remarkId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID reportId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        server.expect(once(), requestTo("http://notification-service/internal/notifications"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Secret", INTERNAL_SECRET))
                .andExpect(jsonPath("$.type").value("REMARK_REPORT"))
                .andExpect(jsonPath("$.recipientUserId").value(recipientUserId.toString()))
                .andExpect(jsonPath("$.remarkId").value(remarkId.toString()))
                .andExpect(jsonPath("$.reportId").value(reportId.toString()))
                .andExpect(jsonPath("$.body").value("Ready"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        client.sendReportNotification(recipientUserId, remarkId, reportId, "REM-2026-00001", "Ready");

        server.verify();
    }
}
