package com.remark.platform.dto;

import com.remark.platform.entity.RoleCode;
import com.remark.platform.entity.UserStatus;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID userId,
        String email,
        String fullName,
        String position,
        UUID branchId,
        UserStatus status,
        Set<RoleCode> roles
) {
}
