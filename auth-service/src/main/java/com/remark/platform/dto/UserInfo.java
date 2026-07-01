package com.remark.platform.dto;

import com.remark.platform.client.RoleCode;

import java.util.Set;
import java.util.UUID;

public record UserInfo(
        UUID userId,
        String email,
        String fullName,
        String position,
        UUID branchId,
        Set<RoleCode> roles
) {
}
