package com.remark.platform.service;

import java.util.UUID;

public record RemarkResolvedApplicationEvent(
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
