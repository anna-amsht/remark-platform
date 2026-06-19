package com.remark.platform.dto;

import com.remark.platform.entity.RoleCode;
import com.remark.platform.entity.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String position,
        UUID branchId,
        UserStatus status,
        Set<RoleCode> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
