package com.remark.platform.events;

import java.time.Instant;
import java.util.UUID;

public record RemarkResolvedMessage(
        UUID eventId,
        Instant timestamp,
        UUID remarkId,
        String remarkNumber,
        UUID branchId,
        UUID createdByUserId,
        String creatorEmail,
        String objectBuilding,
        String resolutionMessage,
        UUID resolvedByUserId
) {
}
