package com.remark.platform.service;

import com.remark.platform.config.MailProperties;
import com.remark.platform.dto.NotificationResponse;
import com.remark.platform.dto.SendNotificationRequest;
import com.remark.platform.entity.DeliveryStatus;
import com.remark.platform.entity.Notification;
import com.remark.platform.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailNotificationSender emailNotificationSender;
    private final MailProperties mailProperties;

    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        Notification notification = new Notification();
        notification.setType(request.type());
        notification.setRecipientEmail(request.recipientEmail().trim().toLowerCase());
        notification.setSubject(request.subject().trim());
        notification.setBody(request.body().trim());
        notification.setStatus(DeliveryStatus.PENDING);

        Notification saved = notificationRepository.save(notification);

        try {
            emailNotificationSender.send(saved);
            saved.setStatus(mailProperties.enabled() ? DeliveryStatus.SENT : DeliveryStatus.LOGGED);
            saved.setSentAt(Instant.now());
        } catch (Exception ex) {
            saved.setStatus(DeliveryStatus.FAILED);
            saved.setErrorMessage(ex.getMessage());
        }

        Notification result = notificationRepository.save(saved);
        return toResponse(result);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getRecipientEmail(),
                notification.getSubject(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}
