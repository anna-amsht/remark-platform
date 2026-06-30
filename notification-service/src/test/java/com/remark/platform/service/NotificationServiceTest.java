package com.remark.platform.service;

import com.remark.platform.dto.SendNotificationRequest;
import com.remark.platform.entity.DeliveryStatus;
import com.remark.platform.entity.Notification;
import com.remark.platform.entity.NotificationType;
import com.remark.platform.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void send_persistsInAppNotification() {
        UUID recipientId = UUID.randomUUID();
        UUID remarkId = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();
        var request = new SendNotificationRequest(
                NotificationType.REMARK_REPORT,
                recipientId,
                remarkId,
                reportId,
                " Отчёт готов ",
                " PDF доступен "
        );

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(UUID.randomUUID());
            notification.setCreatedAt(Instant.parse("2026-06-30T12:00:00Z"));
            return notification;
        });

        var response = notificationService.send(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();

        assertThat(saved.getRecipientUserId()).isEqualTo(recipientId);
        assertThat(saved.getRemarkId()).isEqualTo(remarkId);
        assertThat(saved.getReportId()).isEqualTo(reportId);
        assertThat(saved.getSubject()).isEqualTo("Отчёт готов");
        assertThat(saved.getBody()).isEqualTo("PDF доступен");
        assertThat(saved.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(response.recipientUserId()).isEqualTo(recipientId);
    }

    @Test
    void findForUser_returnsNotificationsSortedByRepository() {
        UUID userId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setType(NotificationType.REMARK_REPORT);
        notification.setRecipientUserId(userId);
        notification.setSubject("Subject");
        notification.setBody("Body");
        notification.setStatus(DeliveryStatus.DELIVERED);
        notification.setCreatedAt(Instant.parse("2026-06-30T12:00:00Z"));

        when(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(notification));

        var responses = notificationService.findForUser(userId);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().subject()).isEqualTo("Subject");
    }
}
