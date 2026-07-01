package com.remark.platform.controller;

import com.remark.platform.dto.NotificationResponse;
import com.remark.platform.dto.SendNotificationRequest;
import com.remark.platform.entity.NotificationType;
import com.remark.platform.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationController notificationController = new NotificationController(notificationService);
    private final InternalNotificationController internalNotificationController =
            new InternalNotificationController(notificationService);

    @Test
    void findForCurrentUserDelegatesToService() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        NotificationResponse notification = notification(userId);
        when(notificationService.findForUser(userId)).thenReturn(List.of(notification));

        var response = notificationController.findForCurrentUser(userId);

        assertThat(response).containsExactly(notification);
        verify(notificationService).findForUser(userId);
    }

    @Test
    void sendDelegatesToService() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        SendNotificationRequest request = new SendNotificationRequest(
                NotificationType.REMARK_REPORT,
                userId,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "Subject",
                "Body"
        );
        NotificationResponse notification = notification(userId);
        when(notificationService.send(request)).thenReturn(notification);

        var response = internalNotificationController.send(request);

        assertThat(response).isSameAs(notification);
        verify(notificationService).send(request);
    }

    private static NotificationResponse notification(UUID userId) {
        return new NotificationResponse(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                NotificationType.REMARK_REPORT,
                userId,
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                "Subject",
                "Body",
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }
}
