package com.remark.platform.service;

import com.remark.platform.dto.TokenRefreshResponse;

public record TokenPair(
        TokenRefreshResponse tokenRefreshResponse,
        String refreshToken
) {
}
