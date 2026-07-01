package com.remark.platform.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class NotificationClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient notificationServiceRestClient;
    private final String internalSecret;

    public NotificationClient(
            RestClient notificationServiceRestClient,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.notificationServiceRestClient = notificationServiceRestClient;
        this.internalSecret = internalSecret;
    }

    public void sendReportNotification(
            UUID recipientUserId,
            UUID remarkId,
            UUID reportId,
            String remarkNumber,
            String body
    ) {
        notificationServiceRestClient.post()
                .uri("/internal/notifications")
                .header(INTERNAL_SECRET_HEADER, internalSecret)
                .body(new SendNotificationRequest(
                        "REMARK_REPORT",
                        recipientUserId,
                        remarkId,
                        reportId,
                        "Отчёт по замечанию " + remarkNumber,
                        body
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private record SendNotificationRequest(
            String type,
            UUID recipientUserId,
            UUID remarkId,
            UUID reportId,
            String subject,
            String body
    ) {
    }
}
