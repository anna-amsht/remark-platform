package com.remark.platform.security;

import com.remark.platform.exceptions.InvalidRequestContextException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserContext(
        UUID userId,
        UUID branchId,
        Set<RoleCode> roles
) {
    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(RoleCode... roleCodes) {
        for (RoleCode roleCode : roleCodes) {
            if (roles.contains(roleCode)) {
                return true;
            }
        }
        return false;
    }

    public UUID requireBranchId() {
        if (branchId == null) {
            throw new InvalidRequestContextException("Branch id is required for this operation");
        }
        return branchId;
    }

    public static UserContext fromHeaders(UUID userId, UUID branchId, String rolesHeader) {
        if (userId == null) {
            throw new InvalidRequestContextException("Header X-User-Id is required");
        }
        if (rolesHeader == null || rolesHeader.isBlank()) {
            throw new InvalidRequestContextException("Header X-User-Roles is required");
        }

        Set<RoleCode> roles = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(RoleCode::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (roles.isEmpty()) {
            throw new InvalidRequestContextException("Header X-User-Roles is required");
        }

        return new UserContext(userId, branchId, roles);
    }
}
