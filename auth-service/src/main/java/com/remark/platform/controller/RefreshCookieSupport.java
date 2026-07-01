package com.remark.platform.controller;

import com.remark.platform.config.CookieProperties;
import com.remark.platform.config.JwtProperties;
import org.springframework.http.ResponseCookie;

public final class RefreshCookieSupport {

    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    public static final String AUTH_PATH = "/api/v1/auth";

    private RefreshCookieSupport() {
    }

    public static ResponseCookie createRefreshCookie(
            String refreshToken,
            CookieProperties cookieProperties,
            JwtProperties jwtProperties
    ) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path(AUTH_PATH)
                .maxAge(jwtProperties.refreshTokenTtl())
                .sameSite("Lax")
                .build();
    }

    public static ResponseCookie clearRefreshCookie(CookieProperties cookieProperties) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path(AUTH_PATH)
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }
}
