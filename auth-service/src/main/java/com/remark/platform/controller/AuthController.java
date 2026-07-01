package com.remark.platform.controller;

import com.remark.platform.config.CookieProperties;
import com.remark.platform.config.JwtProperties;
import com.remark.platform.dto.LoginRequest;
import com.remark.platform.dto.LoginResponse;
import com.remark.platform.dto.TokenRefreshResponse;
import com.remark.platform.exceptions.InvalidRefreshTokenException;
import com.remark.platform.service.AuthService;
import com.remark.platform.service.AuthSession;
import com.remark.platform.service.TokenPair;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthSession session = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, RefreshCookieSupport.createRefreshCookie(
                        session.refreshToken(),
                        cookieProperties,
                        jwtProperties
                ).toString())
                .body(session.loginResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            @CookieValue(name = RefreshCookieSupport.REFRESH_TOKEN_COOKIE, required = false) String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        TokenPair tokenPair = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, RefreshCookieSupport.createRefreshCookie(
                        tokenPair.refreshToken(),
                        cookieProperties,
                        jwtProperties
                ).toString())
                .body(tokenPair.tokenRefreshResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieSupport.REFRESH_TOKEN_COOKIE, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, RefreshCookieSupport.clearRefreshCookie(cookieProperties).toString())
                .build();
    }
}
