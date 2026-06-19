package com.remark.platform.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {
}
