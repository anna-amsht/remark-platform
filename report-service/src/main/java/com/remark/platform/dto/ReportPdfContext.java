package com.remark.platform.dto;

import java.time.Instant;
import java.time.LocalDate;

public record ReportPdfContext(
        String remarkNumber,
        String applicantFullName,
        String branchName,
        String objectBuilding,
        String description,
        LocalDate discoveredAt,
        String responsibleOrganization,
        Instant resolvedAt,
        String resolutionMessage
) {
}
