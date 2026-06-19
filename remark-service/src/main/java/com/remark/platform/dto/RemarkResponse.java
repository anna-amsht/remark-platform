package com.remark.platform.dto;

import com.remark.platform.entity.RemarkStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RemarkResponse(
        UUID id,
        String remarkNumber,
        UUID branchId,
        String objectBuilding,
        String description,
        LocalDate discoveredAt,
        UUID createdByUserId,
        UUID assignedMasterId,
        RemarkStatus status,
        String resolutionMessage,
        Instant resolvedAt,
        Instant closedAt,
        Instant reportSentAt,
        Instant createdAt,
        Instant updatedAt
) {
}
