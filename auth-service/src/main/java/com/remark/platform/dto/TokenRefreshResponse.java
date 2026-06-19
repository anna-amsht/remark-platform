package com.remark.platform.dto;

public record TokenRefreshResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
