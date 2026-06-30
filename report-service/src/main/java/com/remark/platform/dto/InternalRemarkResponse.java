package com.remark.platform.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InternalRemarkResponse(
        UUID id,
        String remarkNumber,
        UUID branchId,
        String objectBuilding,
        String description,
        LocalDate discoveredAt,
        UUID createdByUserId,
        UUID assignedMasterId,
        UUID assignedByUserId,
        String status,
        String resolutionMessage,
        Instant resolvedAt,
        Instant reportSentAt
) {
}
