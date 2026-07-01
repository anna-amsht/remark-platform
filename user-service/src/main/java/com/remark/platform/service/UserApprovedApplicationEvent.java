package com.remark.platform.service;

import java.util.UUID;

public record UserApprovedApplicationEvent(
        UUID userId,
        String email,
        String fullName
) {
}
