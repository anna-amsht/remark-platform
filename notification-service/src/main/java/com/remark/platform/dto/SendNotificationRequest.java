package com.remark.platform.dto;

import com.remark.platform.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendNotificationRequest(
        @NotNull NotificationType type,
        @NotNull UUID recipientUserId,
        UUID remarkId,
        UUID reportId,
        @NotBlank @Size(max = 500) String subject,
        @NotBlank String body
) {
}
