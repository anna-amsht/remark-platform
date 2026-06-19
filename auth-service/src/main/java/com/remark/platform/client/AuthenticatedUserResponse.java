package com.remark.platform.client;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID userId,
        String email,
        String fullName,
        String position,
        UUID branchId,
        String status,
        Set<RoleCode> roles
) {
}
