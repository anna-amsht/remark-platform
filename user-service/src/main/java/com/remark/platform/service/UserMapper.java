package com.remark.platform.service;

import com.remark.platform.dto.AuthenticatedUserResponse;
import com.remark.platform.dto.UserResponse;
import com.remark.platform.entity.RoleCode;
import com.remark.platform.entity.User;
import com.remark.platform.entity.UserStatus;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

final class UserMapper {

    private UserMapper() {
    }

    static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPosition(),
                user.getBranchId(),
                user.getStatus(),
                extractRoleCodes(user),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    static AuthenticatedUserResponse toAuthenticatedResponse(User user) {
        return new AuthenticatedUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPosition(),
                user.getBranchId(),
                user.getStatus(),
                extractRoleCodes(user)
        );
    }

    private static Set<RoleCode> extractRoleCodes(User user) {
        return user.getRoles().stream()
                .map(role -> role.getCode())
                .sorted(Comparator.comparing(Enum::name))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }
}
