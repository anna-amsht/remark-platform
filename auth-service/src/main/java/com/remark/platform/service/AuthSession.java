package com.remark.platform.service;

import com.remark.platform.dto.LoginResponse;

public record AuthSession(
        LoginResponse loginResponse,
        String refreshToken
) {
}
