package com.remark.platform.service;

import com.remark.platform.client.AuthenticatedUserResponse;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.dto.LoginRequest;
import com.remark.platform.dto.LoginResponse;
import com.remark.platform.dto.TokenRefreshResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserServiceClient userServiceClient;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public AuthSession login(LoginRequest request) {
        AuthenticatedUserResponse user = userServiceClient.authenticate(
                request.email(),
                request.password()
        );
        return createSession(user);
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        var userId = refreshTokenService.validateAndGetUserId(rawRefreshToken);
        refreshTokenService.revoke(rawRefreshToken);

        AuthenticatedUserResponse user = userServiceClient.getActiveUserById(userId);
        String accessToken = jwtService.createAccessToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user.userId());

        TokenRefreshResponse response = new TokenRefreshResponse(
                accessToken,
                "Bearer",
                jwtService.accessTokenTtlSeconds()
        );
        return new TokenPair(response, newRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private AuthSession createSession(AuthenticatedUserResponse user) {
        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user.userId());

        LoginResponse loginResponse = new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.accessTokenTtlSeconds(),
                JwtService.toUserInfo(user)
        );
        return new AuthSession(loginResponse, refreshToken);
    }
}
