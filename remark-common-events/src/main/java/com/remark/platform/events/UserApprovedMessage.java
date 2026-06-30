package com.remark.platform.events;

import java.time.Instant;
import java.util.UUID;

public record UserApprovedMessage(
        UUID eventId,
        Instant timestamp,
        UUID userId,
        String email,
        String fullName
) {
}
