package com.remark.platform.dto;

import java.util.UUID;

public record ReportNotificationRecipient(
        UUID userId,
        RecipientRole role
) {
    public enum RecipientRole {
        APPLICANT,
        ASSIGNER
    }
}
