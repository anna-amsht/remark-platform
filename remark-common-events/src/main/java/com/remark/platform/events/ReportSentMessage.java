package com.remark.platform.events;

import java.time.Instant;
import java.util.UUID;

public record ReportSentMessage(
        UUID eventId,
        Instant timestamp,
        UUID remarkId,
        String remarkNumber
) {
}
