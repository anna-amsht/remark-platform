package com.remark.platform.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
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

    public void sendRemarkResolved(String recipientEmail, String remarkNumber, String objectBuilding) {
        try {
            notificationServiceRestClient.post()
                    .uri("/internal/notifications")
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .body(new SendNotificationRequest(
                            "REMARK_RESOLVED",
                            recipientEmail,
                            "Замечание устранено: " + remarkNumber,
                            "Замечание " + remarkNumber + " по объекту \"" + objectBuilding
                                    + "\" переведено в статус RESOLVED."
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to send remark resolved notification: {}", ex.getMessage());
        }
    }

    private record SendNotificationRequest(
            String type,
            String recipientEmail,
            String subject,
            String body
    ) {
    }
}
