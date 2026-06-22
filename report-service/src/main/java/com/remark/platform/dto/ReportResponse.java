package com.remark.platform.dto;

import java.util.UUID;

public record ReportResponse(
        UUID id,
        UUID remarkId,
        String remarkNumber,
        String fileName
) {
}
