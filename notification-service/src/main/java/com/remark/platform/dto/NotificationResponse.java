package com.remark.platform.dto;

import com.remark.platform.entity.DeliveryStatus;
import com.remark.platform.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String recipientEmail,
        String subject,
        DeliveryStatus status,
        Instant createdAt,
        Instant sentAt
) {
}
