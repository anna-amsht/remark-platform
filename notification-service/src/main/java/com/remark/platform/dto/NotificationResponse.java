package com.remark.platform.dto;

import com.remark.platform.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID recipientUserId,
        UUID remarkId,
        UUID reportId,
        String subject,
        String body,
        Instant createdAt
) {
}
