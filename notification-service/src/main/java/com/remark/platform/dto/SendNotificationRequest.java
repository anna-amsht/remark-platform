package com.remark.platform.dto;

import com.remark.platform.entity.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendNotificationRequest(
        @NotNull NotificationType type,
        @NotBlank @Email String recipientEmail,
        @NotBlank @Size(max = 500) String subject,
        @NotBlank String body
) {
}
