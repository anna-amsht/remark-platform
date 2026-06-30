package com.remark.platform.service;

import com.remark.platform.client.AuthenticatedUserResponse;
import com.remark.platform.client.RoleCode;
import com.remark.platform.client.UserServiceClient;
import com.remark.platform.dto.LoginRequest;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserServiceClient userServiceClient = mock(UserServiceClient.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AuthService authService = new AuthService(userServiceClient, jwtService, refreshTokenService);

    @Test
    void loginCreatesAccessAndRefreshTokens() {
        var user = user();
        when(userServiceClient.authenticate("user@example.com", "password")).thenReturn(user);
        when(jwtService.createAccessToken(user)).thenReturn("access-token");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.createRefreshToken(user.userId())).thenReturn("refresh-token");

        AuthSession session = authService.login(new LoginRequest("user@example.com", "password"));

        assertThat(session.refreshToken()).isEqualTo("refresh-token");
        assertThat(session.loginResponse().accessToken()).isEqualTo("access-token");
        assertThat(session.loginResponse().tokenType()).isEqualTo("Bearer");
        assertThat(session.loginResponse().expiresIn()).isEqualTo(900L);
        assertThat(session.loginResponse().user().email()).isEqualTo("user@example.com");
        verify(userServiceClient).authenticate("user@example.com", "password");
    }

    @Test
    void refreshRevokesOldTokenAndCreatesNewTokenPair() {
        var user = user();
        when(refreshTokenService.validateAndGetUserId("old-refresh")).thenReturn(user.userId());
        when(userServiceClient.getActiveUserById(user.userId())).thenReturn(user);
        when(jwtService.createAccessToken(user)).thenReturn("new-access");
        when(jwtService.accessTokenTtlSeconds()).thenReturn(1200L);
        when(refreshTokenService.createRefreshToken(user.userId())).thenReturn("new-refresh");

        TokenPair pair = authService.refresh("old-refresh");

        assertThat(pair.refreshToken()).isEqualTo("new-refresh");
        assertThat(pair.tokenRefreshResponse().accessToken()).isEqualTo("new-access");
        assertThat(pair.tokenRefreshResponse().tokenType()).isEqualTo("Bearer");
        assertThat(pair.tokenRefreshResponse().expiresIn()).isEqualTo(1200L);
        verify(refreshTokenService).validateAndGetUserId("old-refresh");
        verify(refreshTokenService).revoke("old-refresh");
    }

    @Test
    void logoutRevokesNonBlankRefreshToken() {
        authService.logout("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
    }

    @Test
    void logoutIgnoresBlankRefreshToken() {
        authService.logout(" ");
        authService.logout(null);

        verify(refreshTokenService, never()).revoke(" ");
        verify(refreshTokenService, never()).revoke(null);
    }

    private static AuthenticatedUserResponse user() {
        return new AuthenticatedUserResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "user@example.com",
                "Test User",
                "Engineer",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "ACTIVE",
                Set.of(RoleCode.APPLICANT, RoleCode.MASTER)
        );
    }
}
