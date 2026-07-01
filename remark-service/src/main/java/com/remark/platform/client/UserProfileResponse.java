package com.remark.platform.client;

import com.remark.platform.security.RoleCode;

import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String fullName,
        String position,
        UUID branchId,
        String status,
        Set<RoleCode> roles
) {
}
