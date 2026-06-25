package com.remark.platform.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
            String recipientEmail,
            String remarkNumber,
            String body,
            String attachmentFileName,
            String attachmentContentType,
            String attachmentBase64
    ) {
        notificationServiceRestClient.post()
                .uri("/internal/notifications")
                .header(INTERNAL_SECRET_HEADER, internalSecret)
                .body(new SendNotificationRequest(
                        "REMARK_REPORT",
                        recipientEmail,
                        "Отчет по замечанию " + remarkNumber,
                        body,
                        attachmentFileName,
                        attachmentContentType,
                        attachmentBase64
                ))
                .retrieve()
                .toBodilessEntity();
    }

    private record SendNotificationRequest(
            String type,
            String recipientEmail,
            String subject,
            String body,
            String attachmentFileName,
            String attachmentContentType,
            String attachmentBase64
    ) {
    }
}
