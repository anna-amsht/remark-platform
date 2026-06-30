package com.remark.platform.service;

import com.remark.platform.dto.NotificationResponse;
import com.remark.platform.dto.SendNotificationRequest;
import com.remark.platform.entity.DeliveryStatus;
import com.remark.platform.entity.Notification;
import com.remark.platform.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse send(SendNotificationRequest request) {
        Notification notification = new Notification();
        notification.setType(request.type());
        notification.setRecipientUserId(request.recipientUserId());
        notification.setRemarkId(request.remarkId());
        notification.setReportId(request.reportId());
        notification.setSubject(request.subject().trim());
        notification.setBody(request.body().trim());
        notification.setStatus(DeliveryStatus.DELIVERED);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findForUser(UUID userId) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getRecipientUserId(),
                notification.getRemarkId(),
                notification.getReportId(),
                notification.getSubject(),
                notification.getBody(),
                notification.getCreatedAt()
        );
    }
}
